package com.tokoku.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tokoku.common.TooManyRequestsException;

class LoginRateLimiterTest {

    @Test
    void blocksAfterLimitPerEmailCaseInsensitively() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 1000, 60);
        for (int i = 0; i < 3; i++) limiter.check("Budi@Example.com", "1.1.1." + i);

        assertThatThrownBy(() -> limiter.check("budi@example.com", "9.9.9.9"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, e -> assertThat(e.getRetryAfterSeconds()).isPositive());
        assertThatCode(() -> limiter.check("lain@example.com", "9.9.9.9")).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterLimitPerIpAcrossEmails() {
        LoginRateLimiter limiter = new LoginRateLimiter(1000, 2, 60);
        limiter.check("a@example.com", "5.5.5.5");
        limiter.check("b@example.com", "5.5.5.5");

        assertThatThrownBy(() -> limiter.check("c@example.com", "5.5.5.5")).isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.check("c@example.com", "6.6.6.6")).doesNotThrowAnyException();
    }
}
