package com.stockpilot.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple fixed-window in-memory rate limiter keyed by an arbitrary string
 * (e.g. ip+path). Adequate for a single-instance deployment; swap for a
 * Redis-backed limiter if scaled horizontally.
 */
@Component
public class InMemoryRateLimiter {

    private record Window(long windowStartEpochSec, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Window updated = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartEpochSec() >= windowSeconds) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStartEpochSec(), existing.count() + 1);
        });
        return updated.count() <= maxRequests;
    }
}
