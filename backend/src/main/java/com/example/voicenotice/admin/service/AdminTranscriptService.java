package com.example.voicenotice.admin.service;

import com.example.voicenotice.transcript.dto.TranscriptEditHistoryResponse;
import com.example.voicenotice.transcript.dto.TranscriptUpdateRequest;
import com.example.voicenotice.transcript.entity.TranscriptChunk;
import com.example.voicenotice.transcript.entity.TranscriptEditHistory;
import com.example.voicenotice.transcript.repository.TranscriptChunkRepository;
import com.example.voicenotice.transcript.repository.TranscriptEditHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTranscriptService {

    private final TranscriptChunkRepository transcriptChunkRepository;
    private final TranscriptEditHistoryRepository transcriptEditHistoryRepository;

    @Transactional
    public void updateTranscript(Long transcriptId, TranscriptUpdateRequest request, Long adminId) {
        TranscriptChunk transcriptChunk = transcriptChunkRepository.findById(transcriptId)
                .orElseThrow(() -> new IllegalArgumentException("자막을 찾을 수 없습니다."));

        String beforeText = transcriptChunk.getRawText();
        String afterText = request.text();

        TranscriptEditHistory history = new TranscriptEditHistory(
                transcriptChunk,
                beforeText,
                afterText,
                adminId
        );

        transcriptEditHistoryRepository.save(history);

        transcriptChunk.updateText(afterText);
    }

    @Transactional(readOnly = true)
    public List<TranscriptEditHistoryResponse> getEditHistories(Long transcriptId) {
        return transcriptEditHistoryRepository
                .findByTranscriptChunk_IdOrderByEditedAtDesc(transcriptId)
                .stream()
                .map(TranscriptEditHistoryResponse::from)
                .toList();
    }
}