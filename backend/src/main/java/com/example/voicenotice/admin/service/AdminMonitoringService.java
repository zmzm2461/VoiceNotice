package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.AdminMonitoringDetailResponse;
import com.example.voicenotice.admin.dto.AdminMonitoringResponse;
import com.example.voicenotice.conversation.entity.ConversationMessage;
import com.example.voicenotice.conversation.service.ConversationMessageService;
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
    private final ConversationMessageService conversationMessageService;

    @Transactional(readOnly = true)
    public List<AdminMonitoringResponse> getActiveSessions() {
        return intercomSessionRepository
                .findByStatusInOrderByStartedAtDesc(List.of(SessionStatus.OPEN))
                .stream()
                .map(AdminMonitoringResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminMonitoringDetailResponse getSessionDetail(Long sessionId) {
        IntercomSession session = intercomSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        List<ConversationMessage> messages =
                conversationMessageService.getMessages(sessionId);

        return AdminMonitoringDetailResponse.from(session, messages);
    }
}