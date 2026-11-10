package com.tokoku.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.tokoku.support.ApiTestBase;

@TestPropertySource(properties = { "app.ratelimit.admin-login.per-email=2", "app.ratelimit.login.per-email=50" })
class AdminLoginRateLimitIntegrationTest extends ApiTestBase {

    @Test
    void adminLoginIsLimitedSeparatelyFromStoreLogin() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"%s\",\"password\":\"salah-password-1\"}".formatted(email);
        for (int i = 0; i < 2; i++) send(post("/api/admin/auth/login"), null, body).andExpect(status().isUnauthorized());
        send(post("/api/admin/auth/login"), null, body).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        // kuota login toko untuk email yang sama tidak terpengaruh
        send(post("/api/auth/login"), null, body).andExpect(status().isUnauthorized());
    }
}
