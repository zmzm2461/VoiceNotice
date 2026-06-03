package com.example.voicenotice.admin.dto;

import com.example.voicenotice.session.entity.IntercomSession;

import java.time.LocalDateTime;

public record AdminMonitoringResponse(
        Long sessionId,
        Long deviceId,
        String deviceUid,
        String location,
        String status,
        LocalDateTime startedAt,
        LocalDateTime lastSeenAt
) {
    public static AdminMonitoringResponse from(IntercomSession session) {
        return new AdminMonitoringResponse(
                session.getId(),
                session.getDevice().getId(),
                session.getDevice().getDeviceUid(),
                session.getDevice().getLocation(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getDevice().getLastSeenAt()
        );
    }
}