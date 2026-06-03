package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.AdminDashboardResponse;
import com.example.voicenotice.device.repository.DeviceRepository;
import com.example.voicenotice.intercomlog.repository.IntercomLogRepository;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import com.example.voicenotice.transcript.repository.TranscriptChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final IntercomSessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final IntercomLogRepository intercomLogRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;

    public AdminDashboardResponse getDashboard() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long todaySessionCount = sessionRepository.countByStartedAtBetween(start, end);
        long activeSessionCount = sessionRepository.countByStatus(SessionStatus.OPEN);
        long totalDeviceCount = deviceRepository.count();
        long todayLogCount = intercomLogRepository.countByCreatedAtBetween(start, end);

        List<IntercomSession> closedSessions =
                sessionRepository.findByStartedAtBetweenAndEndedAtIsNotNull(start, end);

        double avgResponse = closedSessions.stream()
                .mapToLong(s -> Duration.between(
                        s.getStartedAt(),
                        s.getEndedAt()
                ).getSeconds())
                .average()
                .orElse(0.0);

        Double avgConfidence = transcriptChunkRepository.findAverageConfidence();
        double sttAccuracy = avgConfidence == null ? 0.0 : avgConfidence * 100;

        return new AdminDashboardResponse(
                todaySessionCount,
                activeSessionCount,
                totalDeviceCount,
                todayLogCount,
                avgResponse,
                Math.round(sttAccuracy * 10) / 10.0
        );
    }
}