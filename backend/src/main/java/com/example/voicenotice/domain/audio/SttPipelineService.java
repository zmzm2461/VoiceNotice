package com.example.voicenotice.domain.audio;

import com.example.voicenotice.domain.notice.NoticeService;
import com.example.voicenotice.domain.transcript.TranscriptService;
import com.example.voicenotice.infra.ai.SttClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class SttPipelineService {

    private final SttClient sttClient;
    private final TranscriptService transcriptService;
    private final NoticeService noticeService;

    public SttPipelineService(SttClient sttClient, TranscriptService transcriptService, NoticeService noticeService) {
        this.sttClient = sttClient;
        this.transcriptService = transcriptService;
        this.noticeService = noticeService;
    }

    @Async
    public void processAssembledWav(String deviceUid, String recordedAt, Path wavPath) {
        Long transcriptId = null;

        try {
            byte[] audioBytes = Files.readAllBytes(wavPath);

            // 1) STT
            String rawText = sttClient.stt(audioBytes, wavPath.getFileName().toString());

            // 2) transcript 저장
            LocalDateTime rec = (recordedAt == null || recordedAt.isBlank())
                    ? null
                    : LocalDateTime.parse(recordedAt);

            transcriptId = transcriptService.create(deviceUid, rawText, rec); // 생성 시 PROCESSING

            // 3) refine + notice 저장
            noticeService.createFromTranscript(transcriptId);

            // 성공 메세지
            transcriptService.markCompleted(transcriptId);

            // (선택) 원본 파일 삭제
            // Files.deleteIfExists(wavPath);

        } catch (Exception e) {
            if (transcriptId != null) {
                transcriptService.markFailed(transcriptId, e.getMessage());
            }
            // 로그 남기기
            e.printStackTrace();
        }
    }
}