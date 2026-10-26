package com.papererp.api.security;

import com.papererp.common.exception.DomainException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Enterprise Rate Limiting and Brute-Force Throttling Service.
 * <p>
 * Supports:
 * 1. Distributed rate limiting via Redis with atomic Lua scripts (for multi-instance clusters).
 * 2. In-memory fallback / standalone mode with bounded memory protection (5,000 entries max).
 * 3. Configurable failure modes: 'fail-closed' (default, safe for production), 'in-memory-fallback', or 'fail-open'.
 * 4. Dual-level protection: Account-level (max 5 failures) and IP-level (max 20 failures).
 */
@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    public static final int DEFAULT_ACCOUNT_MAX_FAILED_ATTEMPTS = 5;
    public static final int DEFAULT_IP_MAX_FAILED_ATTEMPTS = 20;
    private static final int MAX_IN_MEMORY_ENTRIES = 5000;
    private static final String REDIS_KEY_PREFIX = "rate_limit:";

    private static final String RECORD_FAILED_LUA_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            elseif current >= tonumber(ARGV[1]) then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            return current
            """;

    private final StringRedisTemplate redisTemplate;
    private volatile String backend;
    private final String redisFailureMode;
    private final long lockoutDurationSeconds;
    private final DefaultRedisScript<Long> recordFailedScript;

    // Bounded in-memory tracker for standalone mode or fallback
    private final Map<String, AttemptTracker> inMemoryAttempts = new ConcurrentHashMap<>();

    public RateLimitingService() {
        this(null, "in-memory", "fail-closed", 900);
    }

    @Autowired
    public RateLimitingService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${app.rate-limiting.backend:in-memory}") String backend,
            @Value("${app.rate-limiting.redis-failure-mode:fail-closed}") String redisFailureMode,
            @Value("${app.rate-limiting.lockout-duration-seconds:900}") long lockoutDurationSeconds) {
        this.redisTemplate = redisTemplate;
        this.backend = backend != null ? backend.trim().toLowerCase() : "in-memory";
        this.redisFailureMode = redisFailureMode != null ? redisFailureMode.trim().toLowerCase() : "fail-closed";
        this.lockoutDurationSeconds = lockoutDurationSeconds;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RECORD_FAILED_LUA_SCRIPT);
        script.setResultType(Long.class);
        this.recordFailedScript = script;
    }

    @PostConstruct
    public void validateConfigurationOnStartup() {
        if ("redis".equalsIgnoreCase(backend)) {
            if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
                if ("fail-closed".equalsIgnoreCase(redisFailureMode)) {
                    throw new IllegalStateException("CRITICAL_STARTUP_FAILURE: Redis rate-limiting backend requested, but StringRedisTemplate is not configured.");
                } else {
                    log.warn("SECURITY_DEGRADATION_WARNING: Redis backend requested but StringRedisTemplate is missing. Falling back to in-memory.");
                    this.backend = "in-memory";
                    return;
                }
            }
            try {
                var conn = redisTemplate.getConnectionFactory().getConnection();
                String pong = conn.ping();
                conn.close();
                log.info("Redis rate-limiting backend verified at startup. Ping: {}", pong);
            } catch (Exception ex) {
                if ("fail-closed".equalsIgnoreCase(redisFailureMode)) {
                    throw new IllegalStateException("CRITICAL_STARTUP_FAILURE: Cannot connect to Redis at startup and failure-mode is 'fail-closed'. Error: " + ex.getMessage(), ex);
                } else {
                    log.warn("SECURITY_DEGRADATION_WARNING: Cannot connect to Redis at startup. Falling back to in-memory rate limiting. Multi-instance protection is DEGRADED. Error: {}", ex.getMessage());
                    this.backend = "in-memory";
                }
            }
        } else {
            log.info("RateLimitingService initialized with IN-MEMORY backend.");
        }
    }

    public boolean isAllowed(String key) {
        return isAllowed(key, DEFAULT_ACCOUNT_MAX_FAILED_ATTEMPTS);
    }

    public boolean isAllowed(String key, int maxAttempts) {
        if (isRedisActive()) {
            return executeWithRedisFallback(
                    () -> checkAllowedRedis(key, maxAttempts),
                    () -> checkAllowedInMemory(key, maxAttempts),
                    "isAllowed"
            );
        }
        return checkAllowedInMemory(key, maxAttempts);
    }

    public void recordFailedAttempt(String key) {
        recordFailedAttempt(key, DEFAULT_ACCOUNT_MAX_FAILED_ATTEMPTS);
    }

    public void recordFailedAttempt(String key, int maxAttempts) {
        if (isRedisActive()) {
            executeWithRedisFallback(
                    () -> {
                        recordFailedAttemptRedis(key, maxAttempts);
                        return null;
                    },
                    () -> {
                        recordFailedAttemptInMemory(key, maxAttempts);
                        return null;
                    },
                    "recordFailedAttempt"
            );
            return;
        }
        recordFailedAttemptInMemory(key, maxAttempts);
    }

    public void recordSuccessfulAttempt(String key) {
        if (isRedisActive()) {
            executeWithRedisFallback(
                    () -> {
                        redisTemplate.delete(REDIS_KEY_PREFIX + key);
                        return null;
                    },
                    () -> {
                        inMemoryAttempts.remove(key);
                        return null;
                    },
                    "recordSuccessfulAttempt"
            );
            return;
        }
        inMemoryAttempts.remove(key);
    }

    public long getLockoutRemainingSeconds(String key) {
        if (isRedisActive()) {
            return executeWithRedisFallback(
                    () -> getLockoutRemainingSecondsRedis(key),
                    () -> getLockoutRemainingSecondsInMemory(key),
                    "getLockoutRemainingSeconds"
            );
        }
        return getLockoutRemainingSecondsInMemory(key);
    }

    public int getFailedAttempts(String key) {
        if (isRedisActive()) {
            return executeWithRedisFallback(
                    () -> getFailedAttemptsRedis(key),
                    () -> getFailedAttemptsInMemory(key),
                    "getFailedAttempts"
            );
        }
        return getFailedAttemptsInMemory(key);
    }

    // --- Redis Implementation ---

    private boolean checkAllowedRedis(String key, int maxAttempts) {
        String val = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + key);
        if (val == null) {
            return true;
        }
        try {
            int count = Integer.parseInt(val);
            if (count >= maxAttempts) {
                log.warn("Rate limit / brute-force lockout active in Redis for key: {}", key);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void recordFailedAttemptRedis(String key, int maxAttempts) {
        String redisKey = REDIS_KEY_PREFIX + key;
        List<String> keys = Collections.singletonList(redisKey);
        Long newCount = redisTemplate.execute(
                recordFailedScript,
                keys,
                String.valueOf(maxAttempts),
                String.valueOf(lockoutDurationSeconds)
        );
        if (newCount != null && newCount >= maxAttempts) {
            log.warn("Rate limit triggered in Redis for '{}': locked out (failed count: {}) for {}s",
                    key, newCount, lockoutDurationSeconds);
        }
    }

    private long getLockoutRemainingSecondsRedis(String key) {
        String redisKey = REDIS_KEY_PREFIX + key;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }

    private int getFailedAttemptsRedis(String key) {
        String val = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + key);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- In-Memory Implementation ---

    private boolean checkAllowedInMemory(String key, int maxAttempts) {
        AttemptTracker tracker = inMemoryAttempts.get(key);
        if (tracker == null) {
            return true;
        }

        Instant now = Instant.now();
        if (tracker.lockedUntil != null && now.isBefore(tracker.lockedUntil)) {
            log.warn("Rate limit / brute-force lockout active in-memory for key: {}", key);
            return false;
        }

        if (tracker.lockedUntil != null && now.isAfter(tracker.lockedUntil)) {
            inMemoryAttempts.remove(key);
            return true;
        }

        return tracker.failedCount < maxAttempts;
    }

    private void recordFailedAttemptInMemory(String key, int maxAttempts) {
        if (inMemoryAttempts.size() >= MAX_IN_MEMORY_ENTRIES) {
            cleanupExpired();
        }

        inMemoryAttempts.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null) {
                return new AttemptTracker(1, null, now);
            }

            int newCount = existing.failedCount + 1;
            Instant lockedUntil = null;
            if (newCount >= maxAttempts) {
                lockedUntil = now.plusSeconds(lockoutDurationSeconds);
                log.warn("Rate limit triggered in-memory for '{}': locked out due to {} failed attempts until {}", key, newCount, lockedUntil);
            }
            return new AttemptTracker(newCount, lockedUntil, now);
        });
    }

    private long getLockoutRemainingSecondsInMemory(String key) {
        AttemptTracker tracker = inMemoryAttempts.get(key);
        if (tracker != null && tracker.lockedUntil != null) {
            long remaining = tracker.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
        return 0;
    }

    private int getFailedAttemptsInMemory(String key) {
        AttemptTracker tracker = inMemoryAttempts.get(key);
        return tracker != null ? tracker.failedCount : 0;
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupExpired() {
        Instant now = Instant.now();
        inMemoryAttempts.entrySet().removeIf(entry -> {
            AttemptTracker tracker = entry.getValue();
            return (tracker.lockedUntil != null && now.isAfter(tracker.lockedUntil))
                    || (tracker.lockedUntil == null && tracker.lastAttempt.plusSeconds(lockoutDurationSeconds).isBefore(now));
        });
    }

    // --- Failure Handling ---

    private <T> T executeWithRedisFallback(Supplier<T> redisAction, Supplier<T> fallbackAction, String operation) {
        try {
            return redisAction.get();
        } catch (Exception ex) {
            log.error("Redis rate-limiting operation '{}' failed: {}", operation, ex.getMessage());
            return handleRedisFailure(fallbackAction, operation);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T handleRedisFailure(Supplier<T> fallbackAction, String operation) {
        if ("fail-closed".equalsIgnoreCase(redisFailureMode)) {
            log.error("CRITICAL_SECURITY_EVENT: Redis rate limiting failure under fail-closed policy. Rejecting request to prevent brute-force attacks.");
            throw new DomainException(
                    "RATE_LIMITER_UNAVAILABLE",
                    "Authentication security service temporarily unavailable. Fail-closed policy active.",
                    503
            );
        } else if ("in-memory-fallback".equalsIgnoreCase(redisFailureMode)) {
            log.warn("SECURITY_DEGRADATION_WARNING: Redis unavailable for '{}'. Falling back to local in-memory tracker. Multi-instance distributed protection is DEGRADED.", operation);
            return fallbackAction.get();
        } else {
            log.warn("SECURITY_RISK_WARNING: Redis unavailable for '{}'. Allowing request under fail-open policy.", operation);
            return (T) Boolean.TRUE;
        }
    }

    public boolean isRedisActive() {
        return "redis".equalsIgnoreCase(backend) && redisTemplate != null;
    }

    public String getBackend() {
        return backend;
    }

    public String getRedisFailureMode() {
        return redisFailureMode;
    }

    public void setBackend(String backend) {
        this.backend = backend != null ? backend.trim().toLowerCase() : "in-memory";
    }

    private record AttemptTracker(int failedCount, Instant lockedUntil, Instant lastAttempt) {}
}
