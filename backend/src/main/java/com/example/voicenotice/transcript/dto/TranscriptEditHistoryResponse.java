package com.example.voicenotice.transcript.dto;

import com.example.voicenotice.transcript.entity.TranscriptEditHistory;

import java.time.LocalDateTime;

public record TranscriptEditHistoryResponse(
        Long id,
        Long transcriptChunkId,
        String beforeText,
        String afterText,
        Long editedBy,
        LocalDateTime editedAt
) {
    public static TranscriptEditHistoryResponse from(TranscriptEditHistory history) {
        return new TranscriptEditHistoryResponse(
                history.getId(),
                history.getTranscriptChunk().getId(),
                history.getBeforeText(),
                history.getAfterText(),
                history.getEditedBy(),
                history.getEditedAt()
        );
    }
}