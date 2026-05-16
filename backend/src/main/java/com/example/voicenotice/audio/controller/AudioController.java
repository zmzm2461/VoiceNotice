package com.example.voicenotice.audio.controller;

import com.example.voicenotice.audio.dto.ChunkUploadResponse;
import com.example.voicenotice.audio.service.AudioService;
import com.example.voicenotice.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    public record AudioChunkUploadResponse(Long chunkId, Long sessionId, Integer chunkOrder, String message) {}


    @GetMapping("/chunk/last")
    public ResponseEntity<ApiResponse<AudioService.LastChunkInfo>> getLastChunk(
            @RequestParam Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(audioService.getLastChunkInfo(sessionId)));
    }

    @PostMapping(value = "/chunk", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ChunkUploadResponse>> uploadChunk(
            @RequestParam Long sessionId,
            @RequestParam Integer chunkOrder,
            @RequestParam(required = false) Integer durationMs,
            @RequestParam(defaultValue = "false") boolean isLast,
            @RequestPart MultipartFile audioFile
    ) throws Exception {

        ChunkUploadResponse response =
                audioService.uploadChunk(sessionId, chunkOrder, durationMs, isLast, audioFile);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
