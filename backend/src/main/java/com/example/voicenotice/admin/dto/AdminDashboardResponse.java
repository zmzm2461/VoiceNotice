package com.example.voicenotice.admin.dto;

public record AdminDashboardResponse(
        long todaySessionCount,
        long activeSessionCount,
        long totalDeviceCount,
        long todayLogCount,
        double averageResponseSeconds,
        double sttAccuracy
) {
}