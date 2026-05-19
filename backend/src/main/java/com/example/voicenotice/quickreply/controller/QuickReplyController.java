package com.example.voicenotice.quickreply.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.quickreply.dto.QuickReplyResponse;
import com.example.voicenotice.quickreply.service.QuickReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quick-replies")
@RequiredArgsConstructor
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    @GetMapping
    public ApiResponse<List<QuickReplyResponse>> getReplies() {
        return ApiResponse.ok(
                quickReplyService.getReplies()
        );
    }
}