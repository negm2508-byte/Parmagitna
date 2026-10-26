package com.papererp.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papererp.api.filter.JwtAuthenticationFilter;
import com.papererp.api.security.JwtTokenService;
import com.papererp.common.security.SystemRole;
import com.papererp.core.domain.UserEntity;
import com.papererp.core.dto.AuthDto;
import com.papererp.core.service.IdentityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.papererp.api.security.RateLimitingService rateLimitingService;

    @Test
    @DisplayName("POST /api/v1/auth/login - Should successfully authenticate and return token and user profile")
    void shouldLoginSuccessfully() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(rateLimitingService.isAllowed(any())).thenReturn(true);
        when(rateLimitingService.isAllowed(any(), anyInt())).thenReturn(true);

        UserEntity user = new UserEntity(
                tenantId, null, "operator1", "operator1@factory.com",
                "$2a$12$hashedPassword", "محمد أحمد - مشغل خط", SystemRole.PRODUCTION_OPERATOR
        );
        user.setId(userId);

        when(identityService.authenticate("operator1", "ValidPass123!")).thenReturn(user);
        when(jwtTokenService.generateAccessToken(eq(tenantId), any(), eq(userId), eq("operator1@factory.com"), eq(SystemRole.PRODUCTION_OPERATOR)))
                .thenReturn("mocked.access.jwt.token");
        when(jwtTokenService.getAccessTokenValidity()).thenReturn(Duration.ofMinutes(15));
        when(identityService.createRefreshToken(eq(tenantId), eq(userId), any())).thenReturn("mocked_raw_refresh_token");

        AuthDto.LoginRequest request = new AuthDto.LoginRequest("operator1", "ValidPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.access.jwt.token"))
                .andExpect(jsonPath("$.data.user.username").value("operator1"))
                .andExpect(jsonPath("$.data.user.role").value("PRODUCTION_OPERATOR"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/csrf - Should return CSRF token successfully")
    void shouldReturnCsrfToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
