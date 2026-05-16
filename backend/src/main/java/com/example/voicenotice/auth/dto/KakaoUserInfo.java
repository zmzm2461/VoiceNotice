package com.example.voicenotice.auth.dto;

public record KakaoUserInfo(
        String providerUserId,
        String name,
        String email
) {
}