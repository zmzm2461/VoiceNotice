package com.example.voicenotice.stt.dto;

public record RealtimeTranscriptMessage(
        Long sessionId,
        String type,
        String text
) {
}