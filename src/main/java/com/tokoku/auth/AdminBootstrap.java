package com.tokoku.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Membuat satu admin awal dari env jika belum ada admin. Aman dijalankan berulang (idempoten).
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
        this.forceChange = forceChange;
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User(email.trim().toLowerCase(), encoder.encode(password), "Admin", Role.ADMIN);
        if (forceChange) admin.requirePasswordChange(); // password awal dari env dianggap sementara; hanya dev/e2e mematikan
        users.save(admin);
        log.info("Bootstrap admin created");
    }
}
