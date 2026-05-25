package com.example.voicenotice.intercomlog.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.intercomlog.dto.IntercomLogResponse;
import com.example.voicenotice.intercomlog.service.IntercomLogService;
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
    public ResponseEntity<ApiResponse<List<IntercomLogResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(intercomLogService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IntercomLogResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(intercomLogService.get(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<IntercomLogResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String intent
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(intercomLogService.search(keyword, intent))
        );
    }
}