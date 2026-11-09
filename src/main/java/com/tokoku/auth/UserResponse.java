package com.tokoku.auth;

public record UserResponse(Long id, String email, String name, Role role) {
    static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }
}
