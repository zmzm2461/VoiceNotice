package com.example.voicenotice.intercomlog.repository;

import com.example.voicenotice.intercomlog.entity.IntercomLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntercomLogRepository extends JpaRepository<IntercomLog, Long> {

    Optional<IntercomLog> findByFinalTranscript_Id(Long finalTranscriptId);
}