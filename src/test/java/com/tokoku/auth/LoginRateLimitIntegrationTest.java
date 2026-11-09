package com.tokoku.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.tokoku.support.ApiTestBase;

@TestPropertySource(properties = "app.ratelimit.login.per-email=3")
class LoginRateLimitIntegrationTest extends ApiTestBase {

    @Test
    void fourthAttemptForSameEmailIs429WithRetryAfter() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email);
        for (int i = 0; i < 3; i++) {
            send(post("/api/auth/login"), null, body).andExpect(status().isUnauthorized());
        }
        send(post("/api/auth/login"), null, body)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
        // email lain tidak terdampak
        send(post("/api/auth/login"), null, "{\"email\":\"%s\",\"password\":\"x\"}".formatted(uniqueEmail())).andExpect(status().isUnauthorized());
    }
}
