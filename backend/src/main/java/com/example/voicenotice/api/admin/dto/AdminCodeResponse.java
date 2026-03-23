package com.example.voicenotice.api.admin.dto;

public class AdminCodeResponse {

    private final String accessToken;

    public AdminCodeResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}