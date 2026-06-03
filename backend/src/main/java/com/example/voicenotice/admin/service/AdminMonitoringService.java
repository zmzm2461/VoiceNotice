package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.AdminMonitoringResponse;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMonitoringService {

    private final IntercomSessionRepository intercomSessionRepository;

    @Transactional(readOnly = true)
    public List<AdminMonitoringResponse> getActiveSessions() {
        List<SessionStatus> activeStatuses = List.of(SessionStatus.OPEN);

        return intercomSessionRepository
                .findByStatusInOrderByStartedAtDesc(activeStatuses)
                .stream()
                .map(AdminMonitoringResponse::from)
                .toList();
    }
}