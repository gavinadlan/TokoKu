package com.tokoku.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.tokoku.common.TooManyRequestsException;
import com.tokoku.common.UnauthorizedException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Autentikasi admin: realm terpisah dari toko (/api/auth/**). Hanya akun ADMIN yang bisa masuk di sini. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AuthService service;
    private final RefreshTokenService refreshTokens;
    private final LoginRateLimiter limiter;

    public AdminAuthController(AuthService service, RefreshTokenService refreshTokens,
            @Qualifier("adminLoginLimiter") LoginRateLimiter limiter) {
        this.service = service;
        this.refreshTokens = refreshTokens;
        this.limiter = limiter;
    }

    @Operation(summary = "Login admin (hanya akun ADMIN; akun pembeli mendapat 401 generik)")
    @SecurityRequirements
    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        try {
            limiter.check(request.email(), ip);
            TokenResponse res = service.login(request, Realm.ADMIN);
            log.info("Admin login success userId={} ip={}", res.user().id(), ip);
            return res;
        } catch (UnauthorizedException e) {
            log.warn("Admin login failed email={} ip={}", mask(request.email()), ip);
            throw e;
        } catch (TooManyRequestsException e) {
            log.warn("Admin login rate limited email={} ip={}", mask(request.email()), ip);
            throw e;
        }
    }

    @Operation(summary = "Tukar refresh token admin (rotasi); token toko ditolak")
    @SecurityRequirements
    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokens.refresh(request.refreshToken(), Realm.ADMIN);
    }

    @Operation(summary = "Logout admin: cabut refresh token (idempoten)")
    @SecurityRequirements
    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken(), Realm.ADMIN);
    }

    @Operation(summary = "Profil admin yang sedang masuk")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(Long.valueOf(jwt.getSubject()));
    }

    @Operation(summary = "Ganti password admin; wajib untuk admin awal. Mencabut semua refresh token, masuk ulang sesudahnya")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(Long.valueOf(jwt.getSubject()), request);
    }

    /** Email disamarkan di log: huruf pertama dan domain saja. */
    static String mask(String email) {
        String e = email == null ? "" : email.trim();
        int at = e.indexOf('@');
        return at <= 0 ? "***" : e.charAt(0) + "***" + e.substring(at);
    }
}
