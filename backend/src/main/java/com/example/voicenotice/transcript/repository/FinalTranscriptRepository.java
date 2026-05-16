package com.example.voicenotice.transcript.repository;

import com.example.voicenotice.transcript.entity.FinalTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinalTranscriptRepository extends JpaRepository<FinalTranscript, Long> {
    Optional<FinalTranscript> findBySession_Id(Long sessionId);
}
