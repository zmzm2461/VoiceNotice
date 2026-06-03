package com.example.voicenotice.transcript.repository;

import com.example.voicenotice.transcript.entity.TranscriptChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {
    List<TranscriptChunk> findBySession_IdOrderByChunkOrderAsc(Long sessionId);

    @Query("select avg(t.confidence) from TranscriptChunk t where t.confidence is not null")
    Double findAverageConfidence();
}
