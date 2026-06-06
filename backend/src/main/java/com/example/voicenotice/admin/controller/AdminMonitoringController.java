package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.dto.AdminMonitoringDetailResponse;
import com.example.voicenotice.admin.dto.AdminMonitoringResponse;
import com.example.voicenotice.admin.service.AdminMonitoringService;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController {

    private final AdminMonitoringService adminMonitoringService;

    @GetMapping
    public ApiResponse<List<AdminMonitoringResponse>> getActiveSessions() {
        return ApiResponse.ok(adminMonitoringService.getActiveSessions());
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<AdminMonitoringDetailResponse> getSessionDetail(
            @PathVariable Long sessionId
    ) {
        return ApiResponse.ok(adminMonitoringService.getSessionDetail(sessionId));
    }
}