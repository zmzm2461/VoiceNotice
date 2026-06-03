package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.service.AdminIntercomLogService;
import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.intercomlog.dto.IntercomLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/intercom-logs")
public class AdminIntercomLogController {

    private final AdminIntercomLogService adminIntercomLogService;

    @GetMapping
    public ApiResponse<List<IntercomLogResponse>> getAllLogs() {
        return ApiResponse.ok(adminIntercomLogService.getAllLogs());
    }

    @GetMapping("/{logId}")
    public ApiResponse<IntercomLogResponse> getLogDetail(
            @PathVariable Long logId
    ) {
        return ApiResponse.ok(adminIntercomLogService.getLogDetail(logId));
    }

    @GetMapping("/search")
    public ApiResponse<List<IntercomLogResponse>> searchLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String deviceUid,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate date
    ) {
        return ApiResponse.ok(
                adminIntercomLogService.searchLogs(
                        userId,
                        deviceId,
                        deviceUid,
                        status,
                        keyword,
                        date
                )
        );
    }

    @DeleteMapping("/{logId}")
    public ApiResponse<Void> deleteLog(
            @PathVariable Long logId
    ) {
        adminIntercomLogService.deleteLog(logId);

        return ApiResponse.ok(null, "관리자 기록 삭제 완료");
    }

}