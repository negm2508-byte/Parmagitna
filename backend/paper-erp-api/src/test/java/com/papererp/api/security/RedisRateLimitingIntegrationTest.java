package com.papererp.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Redis Distributed Rate Limiting Live Integration Tests")
class RedisRateLimitingIntegrationTest {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private RateLimitingService rateLimiterInstance1;
    private RateLimitingService rateLimiterInstance2;
    private String testUserKey;
    private String testIpKey;

    @BeforeAll
    static void initRedisConnection() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
            connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();
            redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();

            // Check liveness
            String pong = connectionFactory.getConnection().ping();
            Assumptions.assumeTrue("PONG".equalsIgnoreCase(pong), "Local Redis server is live and responding");
        } catch (Exception ex) {
            Assumptions.abort("Redis not reachable on " + REDIS_HOST + ":" + REDIS_PORT + " - skipping live tests: " + ex.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        // Instantiate two separate RateLimitingService instances sharing the exact same Redis backend
        // (Simulating two independent horizontal application pods/nodes)
        rateLimiterInstance1 = new RateLimitingService(redisTemplate, "redis", "fail-closed", 900);
        rateLimiterInstance2 = new RateLimitingService(redisTemplate, "redis", "fail-closed", 900);

        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        testUserKey = "user:test_operator_" + randomSuffix;
        testIpKey = "ip:198.51.100." + (int) (Math.random() * 200 + 1);
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.delete("rate_limit:" + testUserKey);
            redisTemplate.delete("rate_limit:" + testIpKey);
        }
    }

    @Test
    @DisplayName("1. Live Redis: Account lockout after 5 failed attempts across instances")
    void accountLockout_distributedAcrossInstances() {
        // Instance 1 records 3 failures
        for (int i = 1; i <= 3; i++) {
            rateLimiterInstance1.recordFailedAttempt(testUserKey, 5);
        }

        // Instance 2 should see 3 failures and allow attempt 4
        assertEquals(3, rateLimiterInstance2.getFailedAttempts(testUserKey));
        assertTrue(rateLimiterInstance2.isAllowed(testUserKey, 5));

        // Instance 2 records 4th and 5th failure
        rateLimiterInstance2.recordFailedAttempt(testUserKey, 5);
        rateLimiterInstance2.recordFailedAttempt(testUserKey, 5);

        // BOTH instances must now immediately report locked out!
        assertFalse(rateLimiterInstance1.isAllowed(testUserKey, 5), "Instance 1 must reflect lockout");
        assertFalse(rateLimiterInstance2.isAllowed(testUserKey, 5), "Instance 2 must reflect lockout");

        // Lockout TTL must be approximately 900 seconds
        long remaining = rateLimiterInstance1.getLockoutRemainingSeconds(testUserKey);
        assertTrue(remaining > 800 && remaining <= 900, "Remaining lockout TTL should be near 900s: " + remaining);
    }

    @Test
    @DisplayName("2. Live Redis: IP credential stuffing defense across instances (20 attempts limit)")
    void ipLockout_distributedAcrossInstances() {
        // 10 failures from Instance 1
        for (int i = 1; i <= 10; i++) {
            rateLimiterInstance1.recordFailedAttempt(testIpKey, 20);
        }

        // 9 failures from Instance 2
        for (int i = 1; i <= 9; i++) {
            rateLimiterInstance2.recordFailedAttempt(testIpKey, 20);
        }

        // 19 failures total -> allowed
        assertTrue(rateLimiterInstance1.isAllowed(testIpKey, 20));
        assertTrue(rateLimiterInstance2.isAllowed(testIpKey, 20));

        // 20th failure on Instance 1 triggers global lockout
        rateLimiterInstance1.recordFailedAttempt(testIpKey, 20);

        // Instance 2 immediately sees the lockout
        assertFalse(rateLimiterInstance2.isAllowed(testIpKey, 20), "Instance 2 must detect IP lockout initiated by Instance 1");
    }

    @Test
    @DisplayName("3. Live Redis: Successful authentication clears lockout counter globally")
    void successfulLogin_clearsCounterGlobally() {
        // Fail 3 times via Instance 1
        rateLimiterInstance1.recordFailedAttempt(testUserKey, 5);
        rateLimiterInstance1.recordFailedAttempt(testUserKey, 5);
        rateLimiterInstance1.recordFailedAttempt(testUserKey, 5);
        assertEquals(3, rateLimiterInstance2.getFailedAttempts(testUserKey));

        // Successful authentication handled on Instance 2
        rateLimiterInstance2.recordSuccessfulAttempt(testUserKey);

        // Instance 1 immediately reflects reset
        assertEquals(0, rateLimiterInstance1.getFailedAttempts(testUserKey));
        assertTrue(rateLimiterInstance1.isAllowed(testUserKey, 5));
    }

    @Test
    @DisplayName("4. Live Redis: High-concurrency atomic counter verification (no race conditions)")
    void highConcurrency_atomicCounterIncrement() throws Exception {
        int threads = 10;
        int attemptsPerThread = 5;
        int expectedTotal = threads * attemptsPerThread; // 50 attempts

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successfulIncrements = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            // Alternate between instance 1 and instance 2
            RateLimitingService client = (t % 2 == 0) ? rateLimiterInstance1 : rateLimiterInstance2;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < attemptsPerThread; i++) {
                        client.recordFailedAttempt(testUserKey, 100);
                        successfulIncrements.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(expectedTotal, successfulIncrements.get());

        // Verify Redis has the EXACT atomic count with zero lost updates
        int finalCountInRedis = rateLimiterInstance1.getFailedAttempts(testUserKey);
        assertEquals(expectedTotal, finalCountInRedis, "Redis atomic Lua INCR must equal exact total attempts");
    }

    @Test
    @DisplayName("5. Live Redis: Successful account login does not clear independent IP credential stuffing counter")
    void successfulAccountLogin_preservesIpCounter() {
        // Attacker attempts 15 failures on the IP
        for (int i = 0; i < 15; i++) {
            rateLimiterInstance1.recordFailedAttempt(testIpKey, 20);
        }
        assertEquals(15, rateLimiterInstance2.getFailedAttempts(testIpKey));

        // Genuine user logs in successfully on their account
        rateLimiterInstance2.recordSuccessfulAttempt(testUserKey);

        // Account counter is clean (0), but IP counter STILL has 15 failures!
        assertEquals(0, rateLimiterInstance1.getFailedAttempts(testUserKey));
        assertEquals(15, rateLimiterInstance1.getFailedAttempts(testIpKey),
                "IP credential stuffing counter must not be cleared by an account login");
    }
}
