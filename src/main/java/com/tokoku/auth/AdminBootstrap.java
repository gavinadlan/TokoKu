package com.tokoku.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Admin awal dari env. Idempoten. Membuat admin bila belum ada admin sama sekali (password sementara: wajib diganti).
 * Untuk admin ADMIN_EMAIL yang sudah ada, tiap start (hanya bila force-password-change aktif) memberi tanda wajib ganti password bila:
 *  - password tersimpan masih sama dengan ADMIN_PASSWORD (masih memakai password awal dari env), atau
 *  - ADMIN_PASSWORD berbeda dari yang tercatat saat start sebelumnya (env diubah/diputar).
 * Password tersimpan tidak pernah ditimpa dari env.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final boolean forceChange;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password,
            @Value("${app.admin.force-password-change:true}") boolean forceChange) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
        this.forceChange = forceChange;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        User existing = users.findByEmail(email.trim().toLowerCase()).filter(u -> u.getRole() == Role.ADMIN).orElse(null);
        if (existing == null) {
            if (users.existsByRole(Role.ADMIN)) {
                return;
            }
            String hash = encoder.encode(password);
            User admin = new User(email.trim().toLowerCase(), hash, "Admin", Role.ADMIN);
            admin.setBootstrapEnvHash(hash);
            if (forceChange) admin.requirePasswordChange(); // hanya dev/e2e mematikan
            users.save(admin);
            log.info("Bootstrap admin created");
            return;
        }
        if (!forceChange) {
            return;
        }
        boolean stillInitial = encoder.matches(password, existing.getPasswordHash());
        String seen = existing.getBootstrapEnvHash();
        boolean envChanged = seen != null && !encoder.matches(password, seen);
        if (seen == null || envChanged) {
            existing.setBootstrapEnvHash(encoder.encode(password));
        }
        boolean flag = (stillInitial || envChanged) && !existing.isMustChangePassword();
        if (flag) {
            existing.requirePasswordChange();
            log.warn("Admin wajib ganti password: {}", stillInitial ? "masih memakai password awal dari env" : "ADMIN_PASSWORD diubah");
        }
        if (flag || seen == null || envChanged) {
            users.save(existing);
        }
    }
}
