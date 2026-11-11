package com.tokoku.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tokoku.support.ApiTestBase;

/** Admin yang sudah ada di DB: ditandai wajib ganti password bila masih password awal env atau env diubah. */
class AdminBootstrapSyncIntegrationTest extends ApiTestBase {

    private static final String ENV_PW = "password-awal-env-1";

    private User admin(String email, String password) {
        return users.save(new User(email, encoder.encode(password), "Admin", Role.ADMIN));
    }

    private void boot(String email, String envPassword, boolean force) {
        new AdminBootstrap(users, encoder, email, envPassword, force).run(null);
    }

    private User reload(String email) { return users.findByEmail(email).orElseThrow(); }

    @Test
    void existingAdminStillOnEnvPasswordIsFlagged() {
        String email = uniqueEmail();
        admin(email, ENV_PW);
        boot(email, ENV_PW, true);
        User u = reload(email);
        assertThat(u.isMustChangePassword()).isTrue();
        assertThat(encoder.matches(ENV_PW, u.getBootstrapEnvHash())).isTrue();
        assertThat(encoder.matches(ENV_PW, u.getPasswordHash())).isTrue(); // password tidak ditimpa
    }

    @Test
    void adminWhoChangedPasswordIsNotFlaggedOnRestart() {
        String email = uniqueEmail();
        admin(email, "password-milik-admin-1");
        boot(email, ENV_PW, true); // start pertama sesudah upgrade: hanya mencatat sidik env
        boot(email, ENV_PW, true); // restart biasa
        assertThat(reload(email).isMustChangePassword()).isFalse();
        assertThat(reload(email).getBootstrapEnvHash()).isNotNull();
    }

    @Test
    void changedEnvPasswordFlagsOnceAndNeverOverwritesPassword() {
        String email = uniqueEmail();
        User u = admin(email, "password-milik-admin-1");
        u.setBootstrapEnvHash(encoder.encode(ENV_PW));
        users.save(u);

        boot(email, "env-diputar-baru-2", true);
        User after = reload(email);
        assertThat(after.isMustChangePassword()).isTrue();
        assertThat(encoder.matches("password-milik-admin-1", after.getPasswordHash())).isTrue();

        // admin mengganti password: flag hilang, restart dengan env yang sama tidak menandai lagi
        after.changePasswordHash(encoder.encode("password-baru-admin-3"));
        users.save(after);
        boot(email, "env-diputar-baru-2", true);
        assertThat(reload(email).isMustChangePassword()).isFalse();
    }

    @Test
    void disabledForceLeavesExistingAdminAlone() {
        String email = uniqueEmail();
        admin(email, ENV_PW);
        boot(email, ENV_PW, false);
        assertThat(reload(email).isMustChangePassword()).isFalse();
    }
}
