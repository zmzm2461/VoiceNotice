package com.example.voicenotice.intercomlog.repository;

import com.example.voicenotice.intercomlog.entity.IntercomLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntercomLogRepository extends JpaRepository<IntercomLog, Long> {

    Optional<IntercomLog> findByFinalTranscript_Id(Long finalTranscriptId);

    List<IntercomLog> findByVisitorTextContainingOrSummaryContainingOrderByCreatedAtDesc(
            String visitorText,
            String summary
    );

    List<IntercomLog> findByIntentOrderByCreatedAtDesc(String intent);

    List<IntercomLog> findByIntentAndVisitorTextContainingOrIntentAndSummaryContainingOrderByCreatedAtDesc(
            String intent1,
            String visitorText,
            String intent2,
            String summary
    );
}