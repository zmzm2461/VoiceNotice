package com.example.voicenotice.stt.dto;

import java.math.BigDecimal;

public record TranscriptMessage(
        Long sessionId,
        Integer chunkOrder,
        String rawText,
        BigDecimal confidence
) {
}