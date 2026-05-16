package com.example.voicenotice.session.repository;

import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntercomSessionRepository extends JpaRepository<IntercomSession, Long> {
    List<IntercomSession> findByDevice_DeviceUidOrderByStartedAtDesc(String deviceUid);

    Optional<IntercomSession> findTopByDevice_DeviceUidAndStatusOrderByStartedAtDesc(
            String deviceUid,
            SessionStatus status
    );
}
