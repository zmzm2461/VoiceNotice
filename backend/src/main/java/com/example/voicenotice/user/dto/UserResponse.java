package com.example.voicenotice.user.dto;

import com.example.voicenotice.user.entity.User;

public record UserResponse(
        Long userId,
        String provider,
        String name,
        String email,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getProvider(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}