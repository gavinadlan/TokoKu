package com.tokoku.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.support.ApiTestBase;

/** Gate B1: token toko tidak bisa mengakses admin, token admin tidak bisa mengakses endpoint pembeli. */
class RealmSeparationIntegrationTest extends ApiTestBase {

    private String body(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private String adminEmail() {
        String email = uniqueEmail();
        users.save(new User(email, encoder.encode(PASSWORD), "Admin", Role.ADMIN));
        return email;
    }

    private String userEmail() {
        String email = uniqueEmail();
        users.save(new User(email, encoder.encode(PASSWORD), "Pembeli", Role.USER));
        return email;
    }

    @Test
    void adminAccountIsRejectedAtStoreLoginWithTheSameMessageAsWrongPassword() throws Exception {
        String admin = adminEmail();
        String asAdminOnStore = send(post("/api/auth/login"), null, body(admin, PASSWORD)).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = send(post("/api/auth/login"), null, body(admin, "salah-password-1")).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(asAdminOnStore, "$.detail")).isEqualTo(JsonPath.read(wrongPassword, "$.detail"));
    }

    @Test
    void userAccountIsRejectedAtAdminLoginWithTheSameMessage() throws Exception {
        String user = userEmail();
        String wrongRole = send(post("/api/admin/auth/login"), null, body(user, PASSWORD)).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = send(post("/api/admin/auth/login"), null, body(user, "salah-password-1")).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(wrongRole, "$.detail")).isEqualTo(JsonPath.read(wrongPassword, "$.detail"));
    }

    @Test
    void storeTokenCannotReachAdminEndpoints() throws Exception {
        String store = userToken();
        send(get("/api/admin/me"), store, null).andExpect(status().isForbidden());
        send(get("/api/admin/products"), store, null).andExpect(status().isForbidden());
        send(get("/api/admin/orders"), store, null).andExpect(status().isForbidden());
        send(post("/api/categories"), store, "{\"name\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    void adminTokenCannotReachBuyerEndpoints() throws Exception {
        String admin = adminToken();
        send(get("/api/me"), admin, null).andExpect(status().isForbidden());
        send(get("/api/cart"), admin, null).andExpect(status().isForbidden());
        send(get("/api/orders"), admin, null).andExpect(status().isForbidden());
        send(post("/api/orders"), admin, SHIPPING).andExpect(status().isForbidden());
        // endpoint admin dan publik tetap bisa
        send(get("/api/admin/me"), admin, null).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        send(get("/api/products"), admin, null).andExpect(status().isOk());
    }

    @Test
    void tokensCarryRealmAndAdminTokenIsShorterLived() throws Exception {
        String store = send(post("/api/auth/login"), null, body(userEmail(), PASSWORD)).andReturn().getResponse().getContentAsString();
        String admin = send(post("/api/admin/auth/login"), null, body(adminEmail(), PASSWORD)).andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(store, "$.expiresInSeconds")).isEqualTo(30 * 60);
        assertThat((Integer) JsonPath.read(admin, "$.expiresInSeconds")).isEqualTo(15 * 60);
    }

    @Test
    void refreshTokensDoNotCrossRealms() throws Exception {
        String storeLogin = send(post("/api/auth/login"), null, body(userEmail(), PASSWORD)).andReturn().getResponse().getContentAsString();
        String adminLogin = send(post("/api/admin/auth/login"), null, body(adminEmail(), PASSWORD)).andReturn().getResponse().getContentAsString();
        String storeRefresh = JsonPath.read(storeLogin, "$.refreshToken");
        String adminRefresh = JsonPath.read(adminLogin, "$.refreshToken");

        send(post("/api/admin/auth/refresh"), null, "{\"refreshToken\":\"%s\"}".formatted(storeRefresh)).andExpect(status().isUnauthorized());
        send(post("/api/auth/refresh"), null, "{\"refreshToken\":\"%s\"}".formatted(adminRefresh)).andExpect(status().isUnauthorized());

        // penolakan lintas realm tidak mencabut token aslinya
        send(post("/api/auth/refresh"), null, "{\"refreshToken\":\"%s\"}".formatted(storeRefresh)).andExpect(status().isOk());
        send(post("/api/admin/auth/refresh"), null, "{\"refreshToken\":\"%s\"}".formatted(adminRefresh)).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void adminLogoutRevokesAdminRefreshTokenOnly() throws Exception {
        String adminLogin = send(post("/api/admin/auth/login"), null, body(adminEmail(), PASSWORD)).andReturn().getResponse().getContentAsString();
        String adminRefresh = JsonPath.read(adminLogin, "$.refreshToken");
        // logout lewat endpoint toko tidak mencabut token admin
        send(post("/api/auth/logout"), null, "{\"refreshToken\":\"%s\"}".formatted(adminRefresh)).andExpect(status().isNoContent());
        send(post("/api/admin/auth/refresh"), null, "{\"refreshToken\":\"%s\"}".formatted(adminRefresh)).andExpect(status().isOk());
    }

    @Test
    void adminEndpointsStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
}
