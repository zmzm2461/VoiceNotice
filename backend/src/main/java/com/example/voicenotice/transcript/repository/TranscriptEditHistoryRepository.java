package com.example.voicenotice.transcript.repository;

import com.example.voicenotice.transcript.entity.TranscriptEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptEditHistoryRepository extends JpaRepository<TranscriptEditHistory, Long> {

    List<TranscriptEditHistory> findByTranscriptChunk_IdOrderByEditedAtDesc(Long transcriptChunkId);
}