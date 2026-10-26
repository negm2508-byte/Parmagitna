package com.papererp.core.dto;

import com.papererp.common.security.SystemRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record LoginResponse(
            String accessToken,
            long expiresInSeconds,
            UserProfileDto user
    ) {}

    public record TokenRefreshResponse(
            String accessToken,
            long expiresInSeconds
    ) {}

    public record UserProfileDto(
            UUID id,
            UUID tenantId,
            UUID defaultPlantId,
            String username,
            String email,
            String fullNameAr,
            SystemRole role
    ) {}
}
