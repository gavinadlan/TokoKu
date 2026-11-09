package com.tokoku.auth;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tokoku.common.TooManyRequestsException;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * Pembatas percobaan login (token bucket Bucket4j) per email dan per IP, di memori.
 * Batas: state hilang saat restart dan tidak dibagi antar instance; untuk beberapa instance pindah ke Redis.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000; // batasi memori: yang paling lama tidak dipakai dibuang

    private final int perEmail;
    private final int perIp;
    private final Duration window;
    private final Map<String, Bucket> emailBuckets = boundedMap();
    private final Map<String, Bucket> ipBuckets = boundedMap();

    public LoginRateLimiter(@Value("${app.ratelimit.login.per-email:5}") int perEmail,
            @Value("${app.ratelimit.login.per-ip:30}") int perIp,
            @Value("${app.ratelimit.login.window-seconds:60}") long windowSeconds) {
        this.perEmail = perEmail;
        this.perIp = perIp;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Melempar TooManyRequestsException (429) jika batas email atau IP terlampaui. */
    public void check(String email, String ip) {
        ConsumptionProbe byEmail = consume(emailBuckets, email.trim().toLowerCase(), perEmail);
        ConsumptionProbe byIp = consume(ipBuckets, ip, perIp);
        if (!byEmail.isConsumed() || !byIp.isConsumed()) {
            long nanos = Math.max(byEmail.isConsumed() ? 0 : byEmail.getNanosToWaitForRefill(),
                    byIp.isConsumed() ? 0 : byIp.getNanosToWaitForRefill());
            throw new TooManyRequestsException("Too many login attempts, try again later", Math.max(1, Duration.ofNanos(nanos).toSeconds() + 1));
        }
    }

    private ConsumptionProbe consume(Map<String, Bucket> map, String key, int capacity) {
        Bucket bucket = map.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, window)).build());
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private static Map<String, Bucket> boundedMap() {
        return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > MAX_TRACKED_KEYS;
            }
        });
    }
}
