package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.QuickReplyStatisticsResponse;
import com.example.voicenotice.quickreply.repository.QuickReplyUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQuickReplyService {

    private final QuickReplyUsageRepository quickReplyUsageRepository;

    @Transactional(readOnly = true)
    public List<QuickReplyStatisticsResponse> getStatistics() {
        return quickReplyUsageRepository.countByReplyCode()
                .stream()
                .map(row -> new QuickReplyStatisticsResponse(
                        (String) row[0],
                        (String) row[1],
                        (Long) row[2]
                ))
                .toList();
    }
}