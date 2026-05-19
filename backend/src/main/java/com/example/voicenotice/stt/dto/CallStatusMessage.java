package com.example.voicenotice.stt.dto;

public record CallStatusMessage(
        Long sessionId,
        String status,
        String message
) {
}