package com.example.voicenotice.stt.dto;

public record RefineResponse(
        String finalText,
        String summary,
        String category,
        Double confidence
) {
}