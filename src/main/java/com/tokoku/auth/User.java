package com.tokoku.auth;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "bootstrap_env_hash")
    private String bootstrapEnvHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String email, String passwordHash, String name, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
    }

    public void requirePasswordChange() { this.mustChangePassword = true; }
    public String getBootstrapEnvHash() { return bootstrapEnvHash; }
    public void setBootstrapEnvHash(String hash) { this.bootstrapEnvHash = hash; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
