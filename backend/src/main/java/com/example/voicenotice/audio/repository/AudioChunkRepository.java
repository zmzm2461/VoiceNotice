package com.example.voicenotice.audio.repository;

import com.example.voicenotice.audio.entity.AudioChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AudioChunkRepository extends JpaRepository<AudioChunk, Long> {
    List<AudioChunk> findBySession_IdOrderByChunkOrderAsc(Long sessionId);

    Optional<AudioChunk> findTopBySession_IdOrderByChunkOrderDesc(Long sessionId);

    Optional<AudioChunk> findBySession_IdAndChunkOrder(Long sessionId, Integer chunkOrder);
}
