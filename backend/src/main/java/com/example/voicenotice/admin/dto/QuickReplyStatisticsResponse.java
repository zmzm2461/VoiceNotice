package com.example.voicenotice.admin.dto;

public record QuickReplyStatisticsResponse(
        String replyCode,
        String text,
        Long useCount
) {
}