package com.example.voicenotice.api.ingest;

import com.example.voicenotice.api.ingest.dto.TranscriptIngestResponse;
import com.example.voicenotice.domain.notice.NoticeService;
import com.example.voicenotice.domain.transcript.TranscriptService;
import com.example.voicenotice.infra.ai.SttClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ingest")
public class AudioIngestController {

    private final SttClient sttClient;
    private final TranscriptService transcriptService;
    private final NoticeService noticeService;

    public AudioIngestController(SttClient sttClient,
                                 TranscriptService transcriptService,
                                 NoticeService noticeService) {
        this.sttClient = sttClient;
        this.transcriptService = transcriptService;
        this.noticeService = noticeService;
    }

    @PostMapping("/audio")
    public ResponseEntity<TranscriptIngestResponse> ingestAudio(
            @RequestParam String deviceUid,
            @RequestParam(required = false) String recordedAt,
            @RequestPart MultipartFile file
    ) throws Exception {

        LocalDateTime recorded = recordedAt == null ? null : LocalDateTime.parse(recordedAt);

        // 1) STT 호출
        String rawText = sttClient.stt(file.getBytes(), file.getOriginalFilename());

        // 2) 기존 로직 재사용: transcript 저장
        Long transcriptId = transcriptService.create(deviceUid, rawText, recorded);

        // 3) refine + notice 저장
        try {
            Long noticeId = noticeService.createFromTranscript(transcriptId);
            return ResponseEntity.ok(new TranscriptIngestResponse(transcriptId, noticeId, "COMPLETED"));
        } catch (Exception e) {
            return ResponseEntity.ok(new TranscriptIngestResponse(transcriptId, null, "FAILED"));
        }
    }
}
