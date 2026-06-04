package com.example.voicenotice.transcript.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.transcript.dto.TranscriptResponse;
import com.example.voicenotice.transcript.dto.FinalizeResult;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import com.example.voicenotice.transcript.entity.TranscriptChunk;
import com.example.voicenotice.transcript.service.TranscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class TranscriptController {
    private final TranscriptService transcriptService;

    public TranscriptController(TranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    public record PartialResponse(Integer chunkOrder, String text) {}

    public record TranscriptListResponse(
            Long sessionId,
            String status,
            List<PartialResponse> partials,
            String finalText
    ) {}

    public record FinalTranscriptResponse(
            Long sessionId,
            Long logId,
            String mergedText,
            String refinedText,
            String status
    ) {}

    @GetMapping("/{sessionId}/transcripts")
    public ResponseEntity<ApiResponse<TranscriptListResponse>> getPartials(@PathVariable Long sessionId) {
        List<TranscriptChunk> chunks = transcriptService.getChunks(sessionId);

        List<PartialResponse> partials = chunks.stream()
                .map(chunk -> new PartialResponse(chunk.getChunkOrder(), chunk.getRawText()))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.ok(new TranscriptListResponse(sessionId, "OPEN", partials, null))
        );
    }

    @PostMapping("/{sessionId}/finalize")
    public ResponseEntity<ApiResponse<FinalTranscriptResponse>> finalizeSession(@PathVariable Long sessionId) {
        FinalizeResult result = transcriptService.finalizeSessionWithLog(sessionId);

        FinalTranscript finalTranscript = result.finalTranscript();

        return ResponseEntity.ok(ApiResponse.ok(new FinalTranscriptResponse(
                sessionId,
                result.logId(),
                finalTranscript.getMergedText(),
                finalTranscript.getRefinedText(),
                finalTranscript.getStatus().name()
        )));
    }

    @GetMapping("/{sessionId}/final")
    public ResponseEntity<ApiResponse<FinalTranscriptResponse>> getFinal(@PathVariable Long sessionId) {
        FinalTranscript finalTranscript = transcriptService.getFinal(sessionId);

        return ResponseEntity.ok(ApiResponse.ok(new FinalTranscriptResponse(
                sessionId,
                null,
                finalTranscript.getMergedText(),
                finalTranscript.getRefinedText(),
                finalTranscript.getStatus().name()
        )));
    }

    @GetMapping("/{sessionId}/stream")
    public ResponseEntity<ApiResponse<TranscriptResponse>> getStream(@PathVariable Long sessionId) {
        TranscriptResponse response = transcriptService.getTranscript(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}