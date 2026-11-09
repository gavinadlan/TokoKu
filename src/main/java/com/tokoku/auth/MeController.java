package com.tokoku.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Auth")
public class MeController {

    private final AuthService service;

    public MeController(AuthService service) {
        this.service = service;
    }

    // @AuthenticationPrincipal Jwt: Spring Security mengisi token yang sudah terverifikasi
    @Operation(summary = "Profil dan role pengguna yang sedang login")
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(Long.valueOf(jwt.getSubject()));
    }
}
