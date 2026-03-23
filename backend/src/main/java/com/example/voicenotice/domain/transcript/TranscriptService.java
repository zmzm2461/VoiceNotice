package com.example.voicenotice.domain.transcript;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;

    public TranscriptService(TranscriptRepository transcriptRepository) {
        this.transcriptRepository = transcriptRepository;
    }

    @Transactional
    public Long create(String deviceUid, String rawText, LocalDateTime recordedAt) {
        if (deviceUid == null || deviceUid.isBlank())
            throw new IllegalArgumentException("deviceUid is required");

        if (rawText == null || rawText.isBlank())
            throw new IllegalArgumentException("rawText is required");

        Transcript t = new Transcript(deviceUid, rawText, recordedAt);
        transcriptRepository.save(t);
        return t.getId();
    }

    @Transactional(readOnly = true)
    public Transcript getOrThrow(Long id) {
        return transcriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transcript not found: " + id));
    }

    // 완료 처리
    @Transactional
    public void markCompleted(Long id) {
        Transcript t = getOrThrow(id);
        t.markCompleted();
    }

    // 실패 처리
    @Transactional
    public void markFailed(Long id, String message) {
        Transcript t = getOrThrow(id);
        t.markFailed(message);
    }
}