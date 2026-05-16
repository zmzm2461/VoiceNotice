package com.example.voicenotice.notice.dto;

import com.example.voicenotice.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponse {

    private Long id;
    private String category;
    private boolean emergency;
    private String finalText;
    private String summary;
    private LocalDateTime createdAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .category(notice.getCategory())
                .emergency(notice.isEmergency())
                .finalText(notice.getFinalText())
                .summary(notice.getSummary())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}