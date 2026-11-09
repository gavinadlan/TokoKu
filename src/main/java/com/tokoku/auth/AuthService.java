package com.tokoku.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;
import com.tokoku.common.UnauthorizedException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, RefreshTokenService refreshTokens) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        // Password tidak pernah disimpan polos: hanya hash BCrypt
        User u = users.save(new User(email, encoder.encode(r.password()), r.name().trim(), Role.USER));
        return UserResponse.from(u);
    }

    @Transactional
    public TokenResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(found -> encoder.matches(r.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return new TokenResponse(jwt.issue(u), "Bearer", jwt.ttlSeconds(), refreshTokens.issue(u.getId()), UserResponse.from(u));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(users.findById(userId).orElseThrow(() -> new NotFoundException("User not found")));
    }

    /** Ganti password: wajib password lama; semua refresh token dicabut (sesi lain keluar). Access token lama hidup sampai habis (maks 30 menit). */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        u.changePasswordHash(encoder.encode(r.newPassword()));
        refreshTokens.revokeAllForUser(userId);
    }
}
