package com.example.voicenotice.api.admin.dto;

public class AdminCheckResponse {

    private final String message;

    public AdminCheckResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}