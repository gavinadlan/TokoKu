package com.tokoku.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tokoku.support.ApiTestBase;

/** Admin awal (bootstrap) memakai password sementara: semua endpoint admin 403 sampai password diganti. */
class ForcedPasswordChangeIntegrationTest extends ApiTestBase {

    private static final String NEW_PASSWORD = "password-baru-123";

    @Test
    void bootstrapAdminMustChangePasswordBeforeUsingAdminApi() throws Exception {
        String email = uniqueEmail();
        User admin = new User(email, encoder.encode(PASSWORD), "Admin", Role.ADMIN);
        admin.requirePasswordChange();
        users.save(admin);

        String token = adminLogin(email, PASSWORD);
        mvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\"}")).andExpect(status().isForbidden());

        send(post("/api/admin/change-password"), token,
                "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(PASSWORD, NEW_PASSWORD)).andExpect(status().isNoContent());

        String fresh = adminLogin(email, NEW_PASSWORD);
        mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + fresh)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + fresh)).andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void bootstrapCreatesAdminWithFlag() {
        var repo = org.mockito.Mockito.mock(UserRepository.class);
        new AdminBootstrap(repo, encoder, "a@b.c", "x-password-1", true).run(null);
        org.mockito.Mockito.verify(repo).save(org.mockito.ArgumentMatchers.argThat(User::isMustChangePassword));
    }
}
