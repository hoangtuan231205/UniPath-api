package com.example.unipathapi.common.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding window rate limiter.
 * Tracks timestamps of requests within a sliding time window.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    /**
     * Attempts to acquire permission for an event under the specified key.
     *
     * @param key unique identifier (e.g., prefix + client IP)
     * @param maxRequests maximum allowed requests within window
     * @param windowMs window size in milliseconds
     * @return true if request is permitted, false if rate limit exceeded
     */
    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        if (maxRequests <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            // Evict expired timestamps
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Calculates remaining seconds until the oldest request slides out of the window.
     */
    public long getRemainingTimeSeconds(String key, long windowMs) {
        Deque<Long> timestamps = requestLog.get(key);
        if (timestamps == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        synchronized (timestamps) {
            Long oldest = timestamps.peekFirst();
            if (oldest == null) {
                return 0;
            }
            long elapsed = now - oldest;
            long remainingMs = windowMs - elapsed;
            return remainingMs > 0 ? (long) Math.ceil(remainingMs / 1000.0) : 1;
        }
    }

    /**
     * Resets rate limit tracking for a specific key.
     */
    public void reset(String key) {
        requestLog.remove(key);
    }

    /**
     * Clears all tracked rate limit keys (useful in test suites).
     */
    public void clear() {
        requestLog.clear();
    }
}

