package com.adaptor.deadrecall.item.copper;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Coordinates asynchronous LLM requests by query key.
 *
 * <p>A key may have at most one in-flight request. Failed requests enter a bounded retry cooldown,
 * while successful requests may be submitted again immediately after their callback completes.</p>
 */
final class LlmRequestGate {
    private final Set<String> pendingQueries = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> retryAfterMs = new ConcurrentHashMap<>();
    private final long failureRetryDelayMs;
    private final LongSupplier clock;

    LlmRequestGate(long failureRetryDelayMs, LongSupplier clock) {
        if (failureRetryDelayMs < 0L) {
            throw new IllegalArgumentException("failureRetryDelayMs must be non-negative");
        }
        this.failureRetryDelayMs = failureRetryDelayMs;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    boolean tryStart(String queryKey) {
        Objects.requireNonNull(queryKey, "queryKey");
        long now = clock.getAsLong();
        Long retryAt = retryAfterMs.get(queryKey);
        if (retryAt != null) {
            if (retryAt > now) {
                return false;
            }
            retryAfterMs.remove(queryKey, retryAt);
        }

        if (!pendingQueries.add(queryKey)) {
            return false;
        }

        // A failure may have installed a cooldown immediately before its pending marker was removed.
        retryAt = retryAfterMs.get(queryKey);
        if (retryAt != null && retryAt > now) {
            pendingQueries.remove(queryKey);
            return false;
        }
        return true;
    }

    void completeSuccess(String queryKey) {
        retryAfterMs.remove(queryKey);
        pendingQueries.remove(queryKey);
    }

    void completeFailure(String queryKey) {
        long now = clock.getAsLong();
        long retryAt = failureRetryDelayMs > Long.MAX_VALUE - now
                ? Long.MAX_VALUE
                : now + failureRetryDelayMs;
        retryAfterMs.put(queryKey, retryAt);
        pendingQueries.remove(queryKey);
    }

    void cancel(String queryKey) {
        pendingQueries.remove(queryKey);
    }

    void clear() {
        pendingQueries.clear();
        retryAfterMs.clear();
    }

    boolean isPending(String queryKey) {
        return pendingQueries.contains(queryKey);
    }

    long retryAfter(String queryKey) {
        return retryAfterMs.getOrDefault(queryKey, 0L);
    }
}
