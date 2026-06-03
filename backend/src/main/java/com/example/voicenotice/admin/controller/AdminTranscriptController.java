package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.service.AdminTranscriptService;
import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.transcript.dto.TranscriptEditHistoryResponse;
import com.example.voicenotice.transcript.dto.TranscriptUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/transcripts")
public class AdminTranscriptController {

    private final AdminTranscriptService adminTranscriptService;

    @PatchMapping("/{transcriptId}")
    public ApiResponse<Void> updateTranscript(
            @PathVariable Long transcriptId,
            @RequestBody TranscriptUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = (Long) httpRequest.getAttribute("userId");

        adminTranscriptService.updateTranscript(transcriptId, request, adminId);

        return ApiResponse.ok(null, "자막 수정 완료");
    }

    @GetMapping("/{transcriptId}/histories")
    public ApiResponse<List<TranscriptEditHistoryResponse>> getEditHistories(
            @PathVariable Long transcriptId
    ) {
        return ApiResponse.ok(adminTranscriptService.getEditHistories(transcriptId));
    }
}