package com.example.voicenotice.session.repository;

import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IntercomSessionRepository extends JpaRepository<IntercomSession, Long> {
    List<IntercomSession> findByDevice_DeviceUidOrderByStartedAtDesc(String deviceUid);

    Optional<IntercomSession> findTopByDevice_DeviceUidAndStatusOrderByStartedAtDesc(
            String deviceUid,
            SessionStatus status
    );

    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(SessionStatus status);

    List<IntercomSession> findByStartedAtBetweenAndEndedAtIsNotNull(
            LocalDateTime start,
            LocalDateTime end
    );

    List<IntercomSession> findByStatusInOrderByStartedAtDesc(List<SessionStatus> statuses);
}
