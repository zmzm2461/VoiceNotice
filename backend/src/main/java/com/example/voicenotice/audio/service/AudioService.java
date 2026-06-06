package com.example.voicenotice.audio.service;

import com.example.voicenotice.audio.dto.ChunkUploadResponse;
import com.example.voicenotice.audio.entity.AudioChunk;
import com.example.voicenotice.audio.repository.AudioChunkRepository;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.service.SessionService;
import com.example.voicenotice.stt.service.SttOrchestrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class AudioService {
    private final AudioChunkRepository audioChunkRepository;
    private final SessionService sessionService;
    private final SttOrchestrationService sttOrchestrationService;

    @Value("${file.upload-dir:storage/audio}")
    private String uploadDir;

    public AudioService(
            AudioChunkRepository audioChunkRepository,
            SessionService sessionService,
            SttOrchestrationService sttOrchestrationService
    ) {
        this.audioChunkRepository = audioChunkRepository;
        this.sessionService = sessionService;
        this.sttOrchestrationService = sttOrchestrationService;
    }

    public record LastChunkInfo(Long sessionId, Integer lastChunkOrder, Integer nextChunkOrder) {}

    @Transactional(readOnly = true)
    public LastChunkInfo getLastChunkInfo(Long sessionId) {
        sessionService.getOrThrow(sessionId);

        Integer lastChunkOrder = audioChunkRepository.findTopBySession_IdOrderByChunkOrderDesc(sessionId)
                .map(AudioChunk::getChunkOrder)
                .orElse(-1);

        return new LastChunkInfo(sessionId, lastChunkOrder, lastChunkOrder + 1);
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(
            Long sessionId,
            Integer chunkOrder,
            Integer durationMs,
            boolean isLast,
            MultipartFile file
    ) throws IOException {
        IntercomSession session = sessionService.getOrThrow(sessionId);
        Path sessionDir = Path.of(uploadDir, String.valueOf(sessionId));
        Files.createDirectories(sessionDir);

        String original = file.getOriginalFilename() == null ? "chunk.wav" : file.getOriginalFilename();
        String savedName = chunkOrder + "_" + original;
        Path savedPath = sessionDir.resolve(savedName);
        Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);

        AudioChunk audioChunk = audioChunkRepository.findBySession_IdAndChunkOrder(sessionId, chunkOrder)
                .map(existingChunk -> {
                    existingChunk.updateChunkFile(savedName, savedPath.toString(), durationMs);
                    return existingChunk;
                })
                .orElseGet(() -> new AudioChunk(session, chunkOrder, savedName, savedPath.toString(), durationMs));

        AudioChunk savedChunk = audioChunkRepository.save(audioChunk);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sttOrchestrationService.transcribeChunkAsync(
                                savedChunk.getId(),
                                isLast
                        );
                    }
                }
        );

        return new ChunkUploadResponse(
                sessionId,
                chunkOrder,
                null,
                null,
                isLast
        );
    }

}
