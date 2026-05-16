package com.example.voicenotice.intercomlog.dto;

import com.example.voicenotice.intercomlog.entity.IntercomLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IntercomLogResponse {

    private Long id;
    private Long sessionId;
    private String visitorText;
    private String summary;
    private String intent;
    private String status;
    private LocalDateTime createdAt;

    public static IntercomLogResponse from(IntercomLog log) {
        return IntercomLogResponse.builder()
                .id(log.getId())
                .sessionId(log.getSessionId())
                .visitorText(log.getVisitorText())
                .summary(log.getSummary())
                .intent(log.getIntent())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}