package com.tokoku.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.support.ApiTestBase;

class RefreshTokenIntegrationTest extends ApiTestBase {

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private int count(String hash) {
        return jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class, hash);
    }

    private record Session(String email, String access, String refresh) {}

    private Session newSession() throws Exception {
        String email = uniqueEmail();
        users.save(new User(email, encoder.encode(PASSWORD), "R", Role.USER));
        String body = send(post("/api/auth/login"), null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return new Session(email, JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private String refreshBody(String token) { return "{\"refreshToken\":\"%s\"}".formatted(token); }

    @Test
    void storesOnlyHashOfRefreshToken() throws Exception {
        Session s = newSession();
        assertThat(count(s.refresh())).isZero();                                  // nilai polos tidak ada di DB
        assertThat(count(RefreshTokenService.hash(s.refresh()))).isEqualTo(1);    // hanya hash-nya
    }

    @Test
    void refreshRotatesAndIssuesWorkingAccessToken() throws Exception {
        Session s = newSession();
        String body = send(post("/api/auth/refresh"), null, refreshBody(s.refresh())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newAccess = JsonPath.read(body, "$.accessToken");
        String newRefresh = JsonPath.read(body, "$.refreshToken");

        assertThat(newRefresh).isNotEqualTo(s.refresh());
        send(get("/api/me"), newAccess, null).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(s.email()));
        send(post("/api/auth/refresh"), null, refreshBody(newRefresh)).andExpect(status().isOk());
    }

    @Test
    void reusingRotatedTokenIs401AndRevokesTheWholeSession() throws Exception {
        Session s = newSession();
        String body = send(post("/api/auth/refresh"), null, refreshBody(s.refresh())).andReturn().getResponse().getContentAsString();
        String newRefresh = JsonPath.read(body, "$.refreshToken");

        send(post("/api/auth/refresh"), null, refreshBody(s.refresh())).andExpect(status().isUnauthorized()); // token lama dipakai ulang
        send(post("/api/auth/refresh"), null, refreshBody(newRefresh)).andExpect(status().isUnauthorized()); // sesi ikut dicabut
    }

    @Test
    void unknownTokenIs401() throws Exception {
        send(post("/api/auth/refresh"), null, refreshBody("nope")).andExpect(status().isUnauthorized());
        send(post("/api/auth/refresh"), null, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void logoutRevokesTokenAndIsIdempotent() throws Exception {
        Session s = newSession();
        send(post("/api/auth/logout"), null, refreshBody(s.refresh())).andExpect(status().isNoContent());
        send(post("/api/auth/logout"), null, refreshBody(s.refresh())).andExpect(status().isNoContent());
        send(post("/api/auth/logout"), null, refreshBody("never-existed")).andExpect(status().isNoContent());
        send(post("/api/auth/refresh"), null, refreshBody(s.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordNeedsLoginAndCurrentPasswordAndRevokesSessions() throws Exception {
        Session s = newSession();
        String change = "{\"currentPassword\":\"%s\",\"newPassword\":\"new-password-9\"}";

        send(post("/api/auth/change-password"), null, change.formatted(PASSWORD)).andExpect(status().isUnauthorized());
        send(post("/api/auth/change-password"), s.access(), change.formatted("wrong-password")).andExpect(status().isUnauthorized());
        send(post("/api/auth/change-password"), s.access(), "{\"currentPassword\":\"%s\",\"newPassword\":\"short\"}".formatted(PASSWORD))
                .andExpect(status().isBadRequest());

        send(post("/api/auth/change-password"), s.access(), change.formatted(PASSWORD)).andExpect(status().isNoContent());

        send(post("/api/auth/refresh"), null, refreshBody(s.refresh())).andExpect(status().isUnauthorized());
        send(post("/api/auth/login"), null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(s.email(), PASSWORD)).andExpect(status().isUnauthorized());
        login(s.email(), "new-password-9");
    }
}
