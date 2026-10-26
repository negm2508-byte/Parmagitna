package com.papererp.core.service;

import com.papererp.common.context.TenantContext;
import com.papererp.common.exception.DomainException;
import com.papererp.common.exception.EntityNotFoundException;
import com.papererp.common.security.SystemRole;
import com.papererp.core.domain.RefreshTokenEntity;
import com.papererp.core.domain.TenantEntity;
import com.papererp.core.domain.UserEntity;
import com.papererp.core.dto.AuthDto;
import com.papererp.core.repository.RefreshTokenRepository;
import com.papererp.core.repository.TenantRepository;
import com.papererp.core.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public IdentityService(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Transactional
    public UserEntity registerUser(UUID tenantId, UUID defaultPlantId, String username,
                                   String email, String rawPassword, String fullNameAr, SystemRole role) {
        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new DomainException("USER_EXISTS", "Username already exists for this tenant", 409);
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new DomainException("EMAIL_EXISTS", "Email already exists for this tenant", 409);
        }

        String hash = passwordEncoder.encode(rawPassword);
        UserEntity user = new UserEntity(tenantId, defaultPlantId, username, email, hash, fullNameAr, role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity authenticate(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Invalid username or password", 401));

        if (!user.isActive()) {
            throw new DomainException("ACCOUNT_INACTIVE", "Account is deactivated", 403);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", "Invalid username or password", 401);
        }

        return user;
    }

    @Transactional
    public String createRefreshToken(UUID tenantId, UUID userId, Duration validity) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(validity);
        RefreshTokenEntity entity = new RefreshTokenEntity(tenantId, userId, tokenHash, expiresAt, familyId);
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional
    public record RotatedTokenResult(String newRawToken, UserEntity user) {}

    @Transactional
    public RotatedTokenResult rotateRefreshToken(String rawOldToken, Duration validity) {
        String oldHash = hashToken(rawOldToken);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(oldHash)
                .orElseThrow(() -> new DomainException("INVALID_REFRESH_TOKEN", "Invalid refresh token", 401));

        if (existing.isRevoked()) {
            // Token reuse detected! Revoke the entire family to protect the compromised user
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), Instant.now());
            throw new DomainException("REFRESH_TOKEN_REUSE_DETECTED", "Refresh token was already used; session revoked", 401);
        }

        if (existing.isExpired()) {
            existing.revoke();
            throw new DomainException("REFRESH_TOKEN_EXPIRED", "Refresh token expired", 401);
        }

        // Revoke current token
        existing.revoke();

        UserEntity user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", existing.getUserId()));

        if (!user.isActive()) {
            throw new DomainException("ACCOUNT_INACTIVE", "Account is deactivated", 403);
        }

        // Issue new token in same family
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String newRawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String newHash = hashToken(newRawToken);

        RefreshTokenEntity newToken = new RefreshTokenEntity(
                existing.getTenantId(), existing.getUserId(), newHash,
                Instant.now().plus(validity), existing.getFamilyId()
        );
        refreshTokenRepository.save(newToken);

        return new RotatedTokenResult(newRawToken, user);
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(entity -> {
            refreshTokenRepository.revokeFamily(entity.getFamilyId(), Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}
