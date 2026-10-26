package com.papererp.api.controller;

import com.papererp.api.security.JwtTokenService;
import com.papererp.api.security.RateLimitingService;
import com.papererp.common.context.TenantContext;
import com.papererp.common.dto.ApiResponse;
import com.papererp.core.domain.UserEntity;
import com.papererp.core.dto.AuthDto;
import com.papererp.core.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "جلسات العمل وتوثيق الهوية وإدارة رموز التحديث وتجديد الجلسات")
public class AuthController {

    private final IdentityService identityService;
    private final JwtTokenService jwtTokenService;
    private final RateLimitingService rateLimitingService;
    private final Duration refreshTokenValidity;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            IdentityService identityService,
            JwtTokenService jwtTokenService,
            RateLimitingService rateLimitingService,
            @Value("${app.jwt.refresh-expiration-hours:8}") long refreshHours,
            @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.jwt.cookie-same-site:Lax}") String cookieSameSite) {
        this.identityService = identityService;
        this.jwtTokenService = jwtTokenService;
        this.rateLimitingService = rateLimitingService;
        this.refreshTokenValidity = Duration.ofHours(refreshHours);
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/login")
    @Operation(summary = "تسجيل الدخول", description = "التحقق من اسم المستخدم وكلمة المرور وإصدار رمز الوصول وملف تعريف الارتباط الآمن لرمز التحديث")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String remoteAddr = httpRequest.getRemoteAddr();
        String accountKey = "user:" + request.username().trim().toLowerCase();
        String ipKey = "ip:" + remoteAddr;

        if (!rateLimitingService.isAllowed(accountKey, RateLimitingService.DEFAULT_ACCOUNT_MAX_FAILED_ATTEMPTS)) {
            long remaining = rateLimitingService.getLockoutRemainingSeconds(accountKey);
            throw new com.papererp.common.exception.DomainException(
                    "ACCOUNT_LOCKED_OUT",
                    "Too many failed login attempts for this account. Temporarily locked. Please try again in " + remaining + " seconds.",
                    429
            );
        }

        if (!rateLimitingService.isAllowed(ipKey, RateLimitingService.DEFAULT_IP_MAX_FAILED_ATTEMPTS)) {
            long remaining = rateLimitingService.getLockoutRemainingSeconds(ipKey);
            throw new com.papererp.common.exception.DomainException(
                    "IP_LOCKED_OUT",
                    "Too many failed login attempts from this network address. Temporarily blocked. Please try again in " + remaining + " seconds.",
                    429
            );
        }

        UserEntity user;
        try {
            user = identityService.authenticate(request.username(), request.password());
            // Clear account-level failure counter for the authenticated user
            rateLimitingService.recordSuccessfulAttempt(accountKey);
            // Note: Do NOT clear ipKey here. IP-level credential stuffing counters must naturally
            // expire via sliding window TTL, preventing attackers from resetting IP limits via a valid credential.
        } catch (com.papererp.common.exception.DomainException ex) {
            rateLimitingService.recordFailedAttempt(accountKey, RateLimitingService.DEFAULT_ACCOUNT_MAX_FAILED_ATTEMPTS);
            rateLimitingService.recordFailedAttempt(ipKey, RateLimitingService.DEFAULT_IP_MAX_FAILED_ATTEMPTS);
            throw ex;
        }

        String accessToken = jwtTokenService.generateAccessToken(
                user.getTenantId(), user.getDefaultPlantId(), user.getId(), user.getEmail(), user.getRole()
        );

        String rawRefreshToken = identityService.createRefreshToken(user.getTenantId(), user.getId(), refreshTokenValidity);
        setRefreshTokenCookie(response, rawRefreshToken, refreshTokenValidity);

        AuthDto.UserProfileDto profile = new AuthDto.UserProfileDto(
                user.getId(), user.getTenantId(), user.getDefaultPlantId(),
                user.getUsername(), user.getEmail(), user.getFullNameAr(), user.getRole()
        );

        AuthDto.LoginResponse loginResponse = new AuthDto.LoginResponse(
                accessToken,
                jwtTokenService.getAccessTokenValidity().toSeconds(),
                profile
        );

        return ResponseEntity.ok(ApiResponse.ok("تم تسجيل الدخول بنجاح", loginResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "تجديد رمز الوصول", description = "تدوير رمز التحديث وإصدار رمز وصول جديد مع كشف محاولات إعادة الاستخدام")
    public ResponseEntity<ApiResponse<AuthDto.TokenRefreshResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String rawOldToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new com.papererp.common.exception.DomainException(
                        "MISSING_REFRESH_TOKEN", "Refresh token is missing from cookie", 401));

        IdentityService.RotatedTokenResult result = identityService.rotateRefreshToken(rawOldToken, refreshTokenValidity);

        String newAccessToken = jwtTokenService.generateAccessToken(
                result.user().getTenantId(), result.user().getDefaultPlantId(),
                result.user().getId(), result.user().getEmail(), result.user().getRole()
        );

        setRefreshTokenCookie(response, result.newRawToken(), refreshTokenValidity);

        AuthDto.TokenRefreshResponse tokenResponse = new AuthDto.TokenRefreshResponse(
                newAccessToken,
                jwtTokenService.getAccessTokenValidity().toSeconds()
        );

        return ResponseEntity.ok(ApiResponse.ok("تم تجديد رمز الوصول بنجاح", tokenResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "تسجيل الخروج", description = "إبطال عائلة رموز التحديث وإزالة ملفات تعريف الارتباط")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        extractRefreshTokenFromCookie(request).ifPresent(identityService::revokeToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("تم تسجيل الخروج وإبطال الجلسة بنجاح", null));
    }

    @GetMapping("/me")
    @Operation(summary = "بيانات المستخدم الحالي", description = "عرض تفاصيل المستخدم الموثق والمصنع الافتراضي وصلاحيات الدور")
    public ResponseEntity<ApiResponse<AuthDto.UserProfileDto>> me() {
        UUID userId = TenantContext.getRequiredUserId();
        UserEntity user = identityService.getUserById(userId);

        AuthDto.UserProfileDto profile = new AuthDto.UserProfileDto(
                user.getId(), user.getTenantId(), user.getDefaultPlantId(),
                user.getUsername(), user.getEmail(), user.getFullNameAr(), user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/csrf")
    @Operation(summary = "الحصول على رمز CSRF", description = "الحصول على رمز CSRF للتحقق من طلبات تغيير الحالة من تطبيقات الواجهة الأمامية")
    public ResponseEntity<ApiResponse<String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String tokenValue = token != null ? token.getToken() : "N/A";
        return ResponseEntity.ok(ApiResponse.ok(tokenValue));
    }

    private Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String rawToken, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
