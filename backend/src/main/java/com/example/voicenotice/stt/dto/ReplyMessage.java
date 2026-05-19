package com.example.voicenotice.stt.dto;

public record ReplyMessage(
        Long sessionId,
        String sender,
        String text
) {
}