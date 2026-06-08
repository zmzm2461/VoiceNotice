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
    private String deviceUid;
    private String visitorText;
    private String summary;
    private String intent;

    private String status;
    private String connectionStatus;
    private String sttStatus;

    private LocalDateTime createdAt;

    public static IntercomLogResponse from(IntercomLog log) {
        String rawStatus = log.getStatus();

        String connectionStatus;
        String sttStatus;

        if ("CLOSED".equals(rawStatus) || "ENDED".equals(rawStatus)) {
            connectionStatus = "종료";
            sttStatus = "완료";
        } else if ("OPEN".equals(rawStatus)) {
            connectionStatus = "연결중";
            sttStatus = "진행중";
        } else if ("FAILED".equals(rawStatus)) {
            connectionStatus = "실패";
            sttStatus = "실패";
        } else {
            connectionStatus = rawStatus;
            sttStatus = rawStatus;
        }

        return IntercomLogResponse.builder()
                .id(log.getId())
                .sessionId(log.getSessionId())
                .deviceUid(log.getDevice().getDeviceUid())
                .visitorText(log.getVisitorText())
                .summary(log.getSummary())
                .intent(log.getIntent())
                .status(rawStatus)
                .connectionStatus(connectionStatus)
                .sttStatus(sttStatus)
                .createdAt(log.getCreatedAt())
                .build();
    }
}