package com.tokoku.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService service;
    private final RefreshTokenService refreshTokens;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService service, RefreshTokenService refreshTokens,
            @org.springframework.beans.factory.annotation.Qualifier("storeLoginLimiter") LoginRateLimiter rateLimiter) {
        this.service = service;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "Daftar akun customer baru")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @Operation(summary = "Login dan dapatkan JWT access token")
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimiter.check(request.email(), http.getRemoteAddr()); // di prod IP asli via forward-headers-strategy
        return service.login(request);
    }

    @Operation(summary = "Tukar refresh token dengan access token baru; refresh token lama dicabut (rotasi)")
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokens.refresh(request.refreshToken(), Realm.STORE);
    }

    @Operation(summary = "Logout: cabut refresh token (idempoten)")
    @SecurityRequirements
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken(), Realm.STORE);
    }

    @Operation(summary = "Ganti password; mencabut semua refresh token")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(Long.valueOf(jwt.getSubject()), request);
    }
}
