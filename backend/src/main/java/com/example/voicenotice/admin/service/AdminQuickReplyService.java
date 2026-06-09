package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.QuickReplyStatisticsResponse;
import com.example.voicenotice.quickreply.repository.QuickReplyUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQuickReplyService {

    private final QuickReplyUsageRepository quickReplyUsageRepository;

    @Transactional(readOnly = true)
    public List<QuickReplyStatisticsResponse> getStatistics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return quickReplyUsageRepository.countByReplyCodeAndUsedAtBetween(start, end)
                .stream()
                .map(row -> new QuickReplyStatisticsResponse(
                        String.valueOf(row[0]),
                        (String) row[1],
                        (Long) row[2]
                ))
                .toList();
    }
}