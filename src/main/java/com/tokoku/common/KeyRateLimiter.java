package com.tokoku.common;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/** Token bucket per kunci (mis. id pengguna), di memori dan dibatasi jumlah kuncinya. Satu instance saja (lihat LoginRateLimiter). */
public class KeyRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int capacity;
    private final Duration window;
    private final String message;
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> e) { return size() > MAX_TRACKED_KEYS; }
    });

    public KeyRateLimiter(int capacity, Duration window, String message) {
        this.capacity = capacity;
        this.window = window;
        this.message = message;
    }

    public void check(String key) {
        Bucket b = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(l -> l.capacity(capacity).refillGreedy(capacity, window)).build());
        ConsumptionProbe p = b.tryConsumeAndReturnRemaining(1);
        if (!p.isConsumed()) {
            throw new TooManyRequestsException(message, Math.max(1, Duration.ofNanos(p.getNanosToWaitForRefill()).toSeconds() + 1));
        }
    }
}
