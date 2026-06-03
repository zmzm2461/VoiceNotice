package com.example.voicenotice.admin.service;

import com.example.voicenotice.intercomlog.dto.IntercomLogResponse;
import com.example.voicenotice.intercomlog.entity.IntercomLog;
import com.example.voicenotice.intercomlog.repository.IntercomLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminIntercomLogService {

    private final IntercomLogRepository intercomLogRepository;

    public List<IntercomLogResponse> getAllLogs() {
        return intercomLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    public IntercomLogResponse getLogDetail(Long logId) {
        IntercomLog log = intercomLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("호출 로그를 찾을 수 없습니다."));

        return IntercomLogResponse.from(log);
    }

    @Transactional(readOnly = true)
    public List<IntercomLogResponse> searchLogs(
            Long userId,
            Long deviceId,
            String deviceUid,
            String status,
            String keyword,
            LocalDate date
    ) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (date != null) {
            startDateTime = date.atStartOfDay();
            endDateTime = date.plusDays(1).atStartOfDay();
        }

        return intercomLogRepository.searchAdminLogs(
                        userId,
                        deviceId,
                        deviceUid,
                        status,
                        keyword,
                        startDateTime,
                        endDateTime
                )
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    @Transactional
    public void deleteLog(Long logId) {
        IntercomLog log = intercomLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 호출 로그를 찾을 수 없습니다."));

        intercomLogRepository.delete(log);
    }
}