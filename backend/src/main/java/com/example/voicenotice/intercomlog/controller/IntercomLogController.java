package com.example.voicenotice.intercomlog.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.intercomlog.dto.IntercomLogResponse;
import com.example.voicenotice.intercomlog.service.IntercomLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/intercom-logs")
public class IntercomLogController {

    private final IntercomLogService intercomLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IntercomLogResponse>>> getMyLogs(
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return ResponseEntity.ok(
                ApiResponse.ok(intercomLogService.getMyLogs(userId))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IntercomLogResponse>> getMyLog(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return ResponseEntity.ok(
                ApiResponse.ok(intercomLogService.getMyLog(userId, id))
        );
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<IntercomLogResponse>>> searchMyLogs(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return ResponseEntity.ok(
                ApiResponse.ok(intercomLogService.searchMyLogs(userId, keyword))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyLog(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        intercomLogService.deleteMyLog(userId, id);

        return ResponseEntity.ok(
                ApiResponse.ok(null, "기록이 삭제되었습니다.")
        );
    }

}