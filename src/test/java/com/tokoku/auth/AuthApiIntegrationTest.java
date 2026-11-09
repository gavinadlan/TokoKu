package com.tokoku.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tokoku.support.ApiTestBase;

class AuthApiIntegrationTest extends ApiTestBase {

    private static final String PRODUCT = "{\"categoryId\":1,\"name\":\"X\",\"price\":1,\"stock\":1}";

    @Test
    void registerLoginAndMe() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Budi\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = login(email, PASSWORD);

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        String email = uniqueEmail();
        String body = "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"A\"}".formatted(email, PASSWORD);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        String email = uniqueEmail();
        userWith(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void weakPasswordIs400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"short\",\"name\":\"A\"}".formatted(uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withoutTokenIs401ProblemDetail() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotUseAdminEndpoints() throws Exception {
        String token = userToken();
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Z\"}")).andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminEndpoints() throws Exception {
        String token = adminToken();
        mvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    private void userWith(String email) {
        users.save(new User(email, encoder.encode(PASSWORD), "U", Role.USER));
    }
}
