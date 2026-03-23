package com.example.voicenotice.api.ingest;

import com.example.voicenotice.api.ingest.dto.TranscriptIngestRequest;
import com.example.voicenotice.api.ingest.dto.TranscriptIngestResponse;
import com.example.voicenotice.domain.notice.NoticeService;
import com.example.voicenotice.domain.transcript.TranscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingest")
public class TranscriptIngestController {

    private final TranscriptService transcriptService;
    private final NoticeService noticeService;

    public TranscriptIngestController(TranscriptService transcriptService, NoticeService noticeService) {
        this.transcriptService = transcriptService;
        this.noticeService = noticeService;
    }

    @PostMapping("/transcripts")
    public ResponseEntity<TranscriptIngestResponse> ingest(@RequestBody TranscriptIngestRequest req) {
        long transcriptId = transcriptService.create(req.getDeviceUid(), req.getRawText(), req.getRecordedAt());

        try {
            long noticeId = noticeService.createFromTranscript(transcriptId);
            return ResponseEntity.ok(new TranscriptIngestResponse(transcriptId, noticeId, "COMPLETED"));
        } catch (Exception e) {
            // MVP: 실패는 FAILED로만 반환 (나중에 에러코드/로그/재시도 확장)
            return ResponseEntity.ok(new TranscriptIngestResponse(transcriptId, null, "FAILED"));
        }
    }
}
