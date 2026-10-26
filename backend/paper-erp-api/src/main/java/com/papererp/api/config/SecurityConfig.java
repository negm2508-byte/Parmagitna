package com.papererp.api.config;

import com.papererp.api.filter.JwtAuthenticationFilter;
import com.papererp.common.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOriginsStr) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/health",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiErrorResponse err = ApiErrorResponse.of(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    "Authentication required or access token expired",
                                    request.getRequestURI(),
                                    request.getHeader("X-Correlation-ID")
                            );
                            objectMapper.writeValue(response.getOutputStream(), err);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiErrorResponse err = ApiErrorResponse.of(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN",
                                    "You do not have permission to access this resource",
                                    request.getRequestURI(),
                                    request.getHeader("X-Correlation-ID")
                            );
                            objectMapper.writeValue(response.getOutputStream(), err);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Master Data
                        .requestMatchers(HttpMethod.POST, "/api/v1/masterdata/**").hasAnyRole("ADMIN", "PLANT_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/masterdata/**").hasAnyRole("ADMIN", "PLANT_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/masterdata/**").authenticated()

                        // Production & Slitting
                        .requestMatchers("/api/v1/production/**").hasAnyRole("ADMIN", "PLANT_MANAGER", "PRODUCTION_OPERATOR")

                        // Quality Gates
                        .requestMatchers("/api/v1/quality/**").hasAnyRole("ADMIN", "PLANT_MANAGER", "QC_INSPECTOR")

                        // Inventory Ledger, Transfers & Dispatch
                        .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "PLANT_MANAGER", "WAREHOUSE_KEEPER")
                        .requestMatchers("/api/v1/rolls/lookup/**").authenticated()

                        // Secured Actuator
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none';"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID", "X-XSRF-TOKEN", "Idempotency-Key"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "X-XSRF-TOKEN", "Idempotency-Key", "X-Cache-Lookup"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
