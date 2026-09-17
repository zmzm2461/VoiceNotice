package com.example.voicenotice.quickreply.dto;

public record QuickReplySuggestionResponse(
        Integer replyCode,
        String text,
        double score
) {
}