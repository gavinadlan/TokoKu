package com.tokoku.auth;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private String realm;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RefreshToken() {}

    public RefreshToken(Long userId, String tokenHash, Instant expiresAt, Realm realm) {
        this.userId = userId;
        this.realm = realm.claim();
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void revoke() { if (revokedAt == null) revokedAt = Instant.now(); }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
    public Long getUserId() { return userId; }
    public boolean inRealm(Realm r) { return r.claim().equals(realm); }
}
