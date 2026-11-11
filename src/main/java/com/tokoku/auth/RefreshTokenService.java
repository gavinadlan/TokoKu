package com.tokoku.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.common.NotFoundException;
import com.tokoku.common.UnauthorizedException;

/**
 * Refresh token = string acak opaque (bukan JWT). Di database hanya hash SHA-256-nya, jadi kebocoran tabel
 * tidak memberi token yang bisa dipakai. Rotasi: tiap dipakai, token lama dicabut dan diganti yang baru.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final JwtService jwt;
    private final Duration ttl;
    private final Duration adminTtl;

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, JwtService jwt,
            @Value("${app.jwt.refresh-days:14}") long refreshDays,
            @Value("${app.jwt.admin-refresh-hours:12}") long adminRefreshHours) {
        this.tokens = tokens;
        this.users = users;
        this.jwt = jwt;
        this.ttl = Duration.ofDays(refreshDays);
        this.adminTtl = Duration.ofHours(adminRefreshHours);
    }

    /** Menerbitkan refresh token baru; nilai polos hanya dikembalikan sekali ini. */
    @Transactional
    public String issue(Long userId, Realm realm) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new RefreshToken(userId, hash(raw), Instant.now().plus(realm == Realm.ADMIN ? adminTtl : ttl), realm));
        return raw;
    }

    // noRollbackFor: pencabutan massal saat deteksi pemakaian ulang harus tersimpan walau request ditolak (401)
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String rawToken, Realm realm) {
        RefreshToken t = tokens.findByHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (t.isRevoked()) {
            // Token yang sudah dipakai muncul lagi: kemungkinan dicuri. Cabut semua sesi pengguna ini.
            tokens.revokeAllForUser(t.getUserId(), Instant.now());
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!t.inRealm(realm)) {
            // Token dari realm lain: ditolak generik, tidak dicabut (bukan bukti pencurian)
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (t.isExpired()) {
            throw new UnauthorizedException("Refresh token expired");
        }
        User user = users.findById(t.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));
        // Peran bisa berubah sejak token terbit: realm admin hanya untuk ADMIN, realm toko hanya untuk USER
        if (!AuthRules.allows(user, realm)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        t.revoke();
        return new TokenResponse(jwt.issue(user, realm), "Bearer", jwt.ttlSeconds(realm), issue(user.getId(), realm), UserResponse.from(user));
    }

    /** Idempoten: token tidak dikenal atau sudah dicabut tetap dianggap berhasil. */
    @Transactional
    public void revoke(String rawToken, Realm realm) {
        tokens.findByHashForUpdate(hash(rawToken)).filter(t -> t.inRealm(realm)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        tokens.revokeAllForUser(userId, Instant.now());
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
