package com.example.voicenotice.transcript.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.service.SessionService;
import com.example.voicenotice.stt.service.SttOrchestrationService;
import com.example.voicenotice.transcript.dto.FinalizeResult;
import com.example.voicenotice.transcript.dto.TranscriptResponse;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import com.example.voicenotice.transcript.entity.TranscriptChunk;
import com.example.voicenotice.transcript.repository.FinalTranscriptRepository;
import com.example.voicenotice.transcript.repository.TranscriptChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TranscriptService {
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final FinalTranscriptRepository finalTranscriptRepository;
    private final SessionService sessionService;
    private final SttOrchestrationService sttOrchestrationService;

    public TranscriptService(
            TranscriptChunkRepository transcriptChunkRepository,
            FinalTranscriptRepository finalTranscriptRepository,
            SessionService sessionService,
            SttOrchestrationService sttOrchestrationService
    ) {
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.finalTranscriptRepository = finalTranscriptRepository;
        this.sessionService = sessionService;
        this.sttOrchestrationService = sttOrchestrationService;
    }

    @Transactional(readOnly = true)
    public List<TranscriptChunk> getChunks(Long sessionId) {
        sessionService.getOrThrow(sessionId);
        return transcriptChunkRepository.findBySession_IdOrderByChunkOrderAsc(sessionId);
    }

    @Transactional
    public FinalTranscript finalizeSession(Long sessionId) {
        IntercomSession session = sessionService.close(sessionId);
        return sttOrchestrationService.finalizeSession(session);
    }

    @Transactional(readOnly = true)
    public FinalTranscript getFinal(Long sessionId) {
        return finalTranscriptRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new NotFoundException("Final transcript not found for session: " + sessionId));
    }

    @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(Long sessionId) {
        sessionService.getOrThrow(sessionId);

        List<TranscriptChunk> chunks =
                transcriptChunkRepository.findBySession_IdOrderByChunkOrderAsc(sessionId);

        String partialText = chunks.stream()
                .map(TranscriptChunk::getRawText)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b)
                .trim();

        FinalTranscript finalTranscript =
                finalTranscriptRepository.findBySession_Id(sessionId).orElse(null);

        String finalText = finalTranscript != null
                ? finalTranscript.getRefinedText()
                : null;

        String status = finalTranscript != null
                ? finalTranscript.getStatus().name()
                : "PROCESSING";

        return TranscriptResponse.builder()
                .partialText(partialText)
                .finalText(finalText)
                .status(status)
                .build();
    }

    @Transactional
    public FinalizeResult finalizeSessionWithLog(Long sessionId) {
        IntercomSession session = sessionService.close(sessionId);
        return sttOrchestrationService.finalizeSessionWithLog(session);
    }
}
