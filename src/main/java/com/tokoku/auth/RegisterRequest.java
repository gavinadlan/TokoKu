package com.tokoku.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password, // BCrypt hanya membaca 72 byte pertama
        @NotBlank @Size(max = 100) String name) {}
