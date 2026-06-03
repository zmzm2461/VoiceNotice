package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.dto.QuickReplyStatisticsResponse;
import com.example.voicenotice.admin.service.AdminQuickReplyService;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/quick-replies")
public class AdminQuickReplyController {

    private final AdminQuickReplyService adminQuickReplyService;

    @GetMapping("/statistics")
    public ApiResponse<List<QuickReplyStatisticsResponse>> getStatistics() {
        return ApiResponse.ok(adminQuickReplyService.getStatistics());
    }
}