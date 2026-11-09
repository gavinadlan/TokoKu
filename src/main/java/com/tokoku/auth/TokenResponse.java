package com.tokoku.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
