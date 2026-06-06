package com.example.voicenotice.admin.dto;

import com.example.voicenotice.conversation.entity.ConversationMessage;
import com.example.voicenotice.session.entity.IntercomSession;

import java.time.LocalDateTime;
import java.util.List;

public record AdminMonitoringDetailResponse(
        Long sessionId,
        Long deviceId,
        String deviceUid,
        String location,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        List<MessageItem> messages
) {
    public static AdminMonitoringDetailResponse from(
            IntercomSession session,
            List<ConversationMessage> messages
    ) {
        return new AdminMonitoringDetailResponse(
                session.getId(),
                session.getDevice().getId(),
                session.getDevice().getDeviceUid(),
                session.getDevice().getLocation(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getEndedAt(),
                messages.stream()
                        .map(MessageItem::from)
                        .toList()
        );
    }

    public record MessageItem(
            Long messageId,
            String senderType,
            String messageType,
            String content,
            String originalContent,
            LocalDateTime createdAt
    ) {
        public static MessageItem from(ConversationMessage message) {
            return new MessageItem(
                    message.getId(),
                    message.getSenderType().name(),
                    message.getMessageType().name(),
                    message.getContent(),
                    message.getOriginalContent(),
                    message.getCreatedAt()
            );
        }
    }
}