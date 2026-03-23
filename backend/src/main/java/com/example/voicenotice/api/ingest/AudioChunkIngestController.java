package com.example.voicenotice.api.ingest;

import com.example.voicenotice.api.ingest.dto.ChunkStatusResponse;
import com.example.voicenotice.api.ingest.dto.ChunkUploadResponse;
import com.example.voicenotice.domain.audio.ChunkAssembleService;
import com.example.voicenotice.domain.audio.SttPipelineService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/ingest/audio")
public class AudioChunkIngestController {

    private final ChunkAssembleService chunkAssembleService;
    private final SttPipelineService sttPipelineService;

    public AudioChunkIngestController(ChunkAssembleService chunkAssembleService,
                                      SttPipelineService sttPipelineService) {
        this.chunkAssembleService = chunkAssembleService;
        this.sttPipelineService = sttPipelineService;
    }

    /**
     * ESP32가 raw bytes로 chunk를 전송한다고 가정
     * Content-Type: application/octet-stream
     */
    @PostMapping(value = "/chunks", consumes = "application/octet-stream")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam String deviceUid,
            @RequestParam int chunkIndex,
            @RequestParam int totalChunks,
            @RequestParam(required = false) String recordedAt,
            HttpServletRequest request
    ) throws Exception {

        byte[] chunkBytes = request.getInputStream().readAllBytes();

        ChunkAssembleService.AppendResult r = chunkAssembleService.append(
                uploadId, deviceUid, chunkIndex, totalChunks, chunkBytes
        );

        // 조립 완료되면 파이프라인 실행
        if ("ASSEMBLED".equals(r.status)) {
            Path wav = chunkAssembleService.getFinalPath(uploadId);
            sttPipelineService.processAssembledWav(deviceUid, recordedAt, wav);
        }

        return ResponseEntity.ok(new ChunkUploadResponse(uploadId, r.status, r.nextChunkIndex));
    }

    /**
     * 끊겼다가 재연결될 때, ESP32가 이어서 보내기 위해 상태 조회
     * 응답의 nextChunkIndex부터 다시 보냄
     */
    @GetMapping("/status")
    public ResponseEntity<ChunkStatusResponse> status(@RequestParam String uploadId) {
        ChunkAssembleService.UploadState s = chunkAssembleService.getState(uploadId);

        if (s == null) {
            // 메모리에 상태가 없으면: 아직 시작 전이거나(0부터), 서버 재시작이거나, 만료/삭제된 상태
            return ResponseEntity.ok(new ChunkStatusResponse(uploadId, "NOT_FOUND", 0, null));
        }

        if (s.assembled) {
            return ResponseEntity.ok(new ChunkStatusResponse(uploadId, "ASSEMBLED", s.nextChunkIndex, s.totalChunks));
        }

        return ResponseEntity.ok(new ChunkStatusResponse(uploadId, "UPLOADING", s.nextChunkIndex, s.totalChunks));
    }
}
