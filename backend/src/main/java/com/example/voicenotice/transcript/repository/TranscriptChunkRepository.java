package com.example.voicenotice.transcript.repository;

import com.example.voicenotice.transcript.entity.TranscriptChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {
    List<TranscriptChunk> findBySession_IdOrderByChunkOrderAsc(Long sessionId);
}
