package com.papererp.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rate Limiting & Brute-Force Throttling Unit Tests")
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    @DisplayName("1. New key is allowed initially")
    void newKey_isAllowed() {
        assertTrue(rateLimitingService.isAllowed("user:operator1"));
        assertTrue(rateLimitingService.isAllowed("ip:192.168.1.10"));
    }

    @Test
    @DisplayName("2. Account lockout after 5 failed attempts")
    void accountLockout_afterFiveFailures() {
        String accountKey = "user:target_account";

        for (int i = 1; i <= 4; i++) {
            rateLimitingService.recordFailedAttempt(accountKey, 5);
            assertTrue(rateLimitingService.isAllowed(accountKey, 5), "Should be allowed on attempt " + i);
        }

        // 5th failed attempt triggers lockout
        rateLimitingService.recordFailedAttempt(accountKey, 5);
        assertFalse(rateLimitingService.isAllowed(accountKey, 5), "Should be locked out on 5th attempt");
        assertTrue(rateLimitingService.getLockoutRemainingSeconds(accountKey) > 0);
    }

    @Test
    @DisplayName("3. IP lockout after 20 failed attempts (credential stuffing defense)")
    void ipLockout_afterTwentyFailures() {
        String ipKey = "ip:10.0.0.50";

        for (int i = 1; i <= 19; i++) {
            rateLimitingService.recordFailedAttempt(ipKey, 20);
            assertTrue(rateLimitingService.isAllowed(ipKey, 20));
        }

        // 20th failure triggers IP lockout
        rateLimitingService.recordFailedAttempt(ipKey, 20);
        assertFalse(rateLimitingService.isAllowed(ipKey, 20));
        assertTrue(rateLimitingService.getLockoutRemainingSeconds(ipKey) > 0);
    }

    @Test
    @DisplayName("4. Account and IP keys are independent: IP change does not reset account counter")
    void accountAndIpAreIndependent() {
        String account = "user:victim";
        String ip1 = "ip:1.1.1.1";
        String ip2 = "ip:2.2.2.2";

        // Attacker tries from IP 1 (3 times)
        rateLimitingService.recordFailedAttempt(account, 5);
        rateLimitingService.recordFailedAttempt(account, 5);
        rateLimitingService.recordFailedAttempt(account, 5);
        rateLimitingService.recordFailedAttempt(ip1, 20);

        // Attacker switches to IP 2 and tries 2 more times
        rateLimitingService.recordFailedAttempt(account, 5);
        rateLimitingService.recordFailedAttempt(account, 5); // 5th failure for account!
        rateLimitingService.recordFailedAttempt(ip2, 20);

        // Account is locked out even though IP changed!
        assertFalse(rateLimitingService.isAllowed(account, 5), "Account must remain locked out across IPs");
        assertTrue(rateLimitingService.isAllowed(ip2, 20), "IP2 has only 2 failures, not yet locked");
    }

    @Test
    @DisplayName("5. Successful login resets attempt counter")
    void successfulLogin_resetsCounter() {
        String accountKey = "user:valid_user";

        rateLimitingService.recordFailedAttempt(accountKey, 5);
        rateLimitingService.recordFailedAttempt(accountKey, 5);
        assertEquals(2, rateLimitingService.getFailedAttempts(accountKey));

        rateLimitingService.recordSuccessfulAttempt(accountKey);
        assertEquals(0, rateLimitingService.getFailedAttempts(accountKey));
        assertTrue(rateLimitingService.isAllowed(accountKey, 5));
    }

    @Test
    @DisplayName("6. Redis Failure Mode: 'fail-closed' throws 503 DomainException when Redis is down")
    void redisFailure_failClosed_throws503() {
        org.springframework.data.redis.core.StringRedisTemplate brokenRedis = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.StringRedisTemplate.class
        );
        org.springframework.data.redis.core.ValueOperations<String, String> ops = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.ValueOperations.class
        );
        org.mockito.Mockito.when(brokenRedis.opsForValue()).thenReturn(ops);
        org.mockito.Mockito.when(ops.get(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection refused"));

        RateLimitingService failClosedLimiter = new RateLimitingService(brokenRedis, "redis", "fail-closed", 900);

        com.papererp.common.exception.DomainException ex = assertThrows(
                com.papererp.common.exception.DomainException.class,
                () -> failClosedLimiter.isAllowed("user:anyone", 5)
        );

        assertEquals(503, ex.getHttpStatus());
        assertEquals("RATE_LIMITER_UNAVAILABLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("7. Redis Failure Mode: 'in-memory-fallback' falls back to in-memory tracker when Redis fails")
    void redisFailure_inMemoryFallback_degradesGracefully() {
        org.springframework.data.redis.core.StringRedisTemplate brokenRedis = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.StringRedisTemplate.class
        );
        org.springframework.data.redis.core.ValueOperations<String, String> ops = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.ValueOperations.class
        );
        org.mockito.Mockito.when(brokenRedis.opsForValue()).thenReturn(ops);
        org.mockito.Mockito.when(ops.get(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Timeout"));

        RateLimitingService fallbackLimiter = new RateLimitingService(brokenRedis, "redis", "in-memory-fallback", 900);

        // First attempt allowed via in-memory fallback
        assertTrue(fallbackLimiter.isAllowed("user:fallback_user", 5));
    }

    @Test
    @DisplayName("8. Redis Failure Mode: 'fail-open' allows requests under fail-open policy")
    void redisFailure_failOpen_allowsRequest() {
        org.springframework.data.redis.core.StringRedisTemplate brokenRedis = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.StringRedisTemplate.class
        );
        org.springframework.data.redis.core.ValueOperations<String, String> ops = org.mockito.Mockito.mock(
                org.springframework.data.redis.core.ValueOperations.class
        );
        org.mockito.Mockito.when(brokenRedis.opsForValue()).thenReturn(ops);
        org.mockito.Mockito.when(ops.get(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection reset"));

        RateLimitingService failOpenLimiter = new RateLimitingService(brokenRedis, "redis", "fail-open", 900);

        assertTrue(failOpenLimiter.isAllowed("user:failopen_user", 5));
    }
}
