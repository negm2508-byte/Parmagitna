package com.papererp.api.security;

import com.papererp.common.context.TenantDetails;
import com.papererp.common.security.SystemRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class JwtTokenService {

    private final SecretKey key;
    private final Duration accessTokenValidity;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:15}") long expirationMinutes) {
        // Ensure secret is at least 256 bits (32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 256 bits (32 bytes) of cryptographically secure entropy. Configured secret length: " + keyBytes.length + " bytes."
            );
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = Duration.ofMinutes(expirationMinutes);
    }

    public String generateAccessToken(UUID tenantId, UUID defaultPlantId, UUID userId, String email, SystemRole role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenValidity);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key);

        if (defaultPlantId != null) {
            builder.claim("plant_id", defaultPlantId.toString());
        }

        return builder.compact();
    }

    public Optional<TenantDetails> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
            String email = claims.get("email", String.class);
            String roleStr = claims.get("role", String.class);
            SystemRole role = SystemRole.valueOf(roleStr);

            String plantStr = claims.get("plant_id", String.class);
            UUID defaultPlantId = plantStr != null ? UUID.fromString(plantStr) : null;

            return Optional.of(new TenantDetails(tenantId, defaultPlantId, userId, email, Set.of(role)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }
}
