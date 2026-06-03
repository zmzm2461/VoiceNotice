package com.example.voicenotice.admin.dto;

import com.example.voicenotice.device.entity.Device;

import java.time.LocalDateTime;

public record AdminDeviceResponse(
        Long deviceId,
        String deviceUid,
        String location,
        String status,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminDeviceResponse from(Device device) {
        return new AdminDeviceResponse(
                device.getId(),
                device.getDeviceUid(),
                device.getLocation(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}