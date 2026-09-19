package com.mip.security.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token buckets per client key. Suits a single instance; swap for a shared
 * store (e.g. Redis) when the API runs on multiple nodes.
 */
@Component
public class RateLimiterRegistry {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /** @return negative when a token was granted; otherwise seconds to wait before retrying. */
    public long tryConsume(String key, int limit, int windowSeconds) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit, windowSeconds));
        return bucket.tryConsume();
    }

    /** Test hook and operational reset. */
    public void clear() {
        buckets.clear();
    }

    @Scheduled(fixedDelay = 600_000)
    void purgeIdleBuckets() {
        long cutoff = System.nanoTime() - 900L * 1_000_000_000;
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessNanos() < cutoff);
    }

    static final class TokenBucket {

        private final int capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefillNanos;
        private volatile long lastAccessNanos;

        TokenBucket(int capacity, int windowSeconds) {
            this.capacity = capacity;
            this.refillPerNano = (double) capacity / (windowSeconds * 1_000_000_000L);
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
            this.lastAccessNanos = lastRefillNanos;
        }

        synchronized long tryConsume() {
            long now = System.nanoTime();
            lastAccessNanos = now;
            tokens = Math.min(capacity, tokens + (now - lastRefillNanos) * refillPerNano);
            lastRefillNanos = now;
            if (tokens >= 1) {
                tokens -= 1;
                return -1;
            }
            double nanosUntilToken = (1 - tokens) / refillPerNano;
            return Math.max(1, (long) Math.ceil(nanosUntilToken / 1_000_000_000));
        }

        long lastAccessNanos() {
            return lastAccessNanos;
        }
    }
}
