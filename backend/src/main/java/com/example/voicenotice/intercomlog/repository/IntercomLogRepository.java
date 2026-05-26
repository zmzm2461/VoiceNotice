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

    List<IntercomLog> findByDevice_IdInAndVisitorTextContainingOrDevice_IdInAndSummaryContainingOrderByCreatedAtDesc(
            List<Long> deviceIds1,
            String visitorText,
            List<Long> deviceIds2,
            String summary
    );

    List<IntercomLog> findAllByOrderByCreatedAtDesc();

    List<IntercomLog> findByDevice_IdInOrderByCreatedAtDesc(List<Long> deviceIds);

    Optional<IntercomLog> findByIdAndDevice_IdIn(Long id, List<Long> deviceIds);
}