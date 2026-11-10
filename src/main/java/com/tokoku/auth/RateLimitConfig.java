package com.tokoku.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Dua pembatas login terpisah: login admin jauh lebih ketat dan tidak berbagi kuota dengan login toko. */
@Configuration
public class RateLimitConfig {

    @Bean("storeLoginLimiter")
    LoginRateLimiter storeLoginLimiter(@Value("${app.ratelimit.login.per-email:5}") int perEmail,
            @Value("${app.ratelimit.login.per-ip:30}") int perIp,
            @Value("${app.ratelimit.login.window-seconds:60}") long windowSeconds) {
        return new LoginRateLimiter(perEmail, perIp, windowSeconds);
    }

    @Bean("adminLoginLimiter")
    LoginRateLimiter adminLoginLimiter(@Value("${app.ratelimit.admin-login.per-email:3}") int perEmail,
            @Value("${app.ratelimit.admin-login.per-ip:10}") int perIp,
            @Value("${app.ratelimit.admin-login.window-seconds:60}") long windowSeconds) {
        return new LoginRateLimiter(perEmail, perIp, windowSeconds);
    }
}
