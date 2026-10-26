package com.papererp.api.filter;

import com.papererp.api.security.JwtTokenService;
import com.papererp.common.context.TenantContext;
import com.papererp.common.context.TenantDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            extractToken(request)
                    .flatMap(jwtTokenService::parseToken)
                    .ifPresent(this::setSecurityAndTenantContext);

            filterChain.doFilter(request, response);
        } finally {
            // Strict defense-in-depth: ThreadLocal MUST be cleared to prevent cross-request leakage
            TenantContext.clear();
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "access_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }

        return Optional.empty();
    }

    private void setSecurityAndTenantContext(TenantDetails details) {
        var authorities = details.roles().stream()
                .map(r -> new SimpleGrantedAuthority(r.asAuthority()))
                .collect(Collectors.toList());

        var auth = new UsernamePasswordAuthenticationToken(details, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.set(details);
    }
}
