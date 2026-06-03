package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.dto.AdminDashboardResponse;
import com.example.voicenotice.admin.service.AdminDashboardService;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getDashboard())
        );
    }
}