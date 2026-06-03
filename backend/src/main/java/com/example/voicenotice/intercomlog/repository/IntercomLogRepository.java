package com.example.voicenotice.intercomlog.repository;

import com.example.voicenotice.intercomlog.entity.IntercomLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT DISTINCT l
    FROM IntercomLog l
    LEFT JOIN DevicePairing dp ON dp.device = l.device
    WHERE (:userId IS NULL OR dp.user.id = :userId)
      AND (:deviceId IS NULL OR l.device.id = :deviceId)
      AND (:deviceUid IS NULL OR l.device.deviceUid LIKE %:deviceUid%)
      AND (:status IS NULL OR l.status = :status)
      AND (:keyword IS NULL
            OR l.visitorText LIKE %:keyword%
            OR l.summary LIKE %:keyword%
            OR l.intent LIKE %:keyword%)
      AND (:startDateTime IS NULL OR l.createdAt >= :startDateTime)
      AND (:endDateTime IS NULL OR l.createdAt < :endDateTime)
    ORDER BY l.createdAt DESC
""")
    List<IntercomLog> searchAdminLogs(
            Long userId,
            Long deviceId,
            String deviceUid,
            String status,
            String keyword,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}