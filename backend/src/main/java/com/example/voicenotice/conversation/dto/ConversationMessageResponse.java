package com.example.voicenotice.conversation.dto;

import com.example.voicenotice.conversation.entity.ConversationMessage;

import java.time.LocalDateTime;

public record ConversationMessageResponse(
        Long messageId,
        Long sessionId,
        String senderType,
        String messageType,
        String content,
        String originalContent,
        LocalDateTime createdAt
) {
    public static ConversationMessageResponse from(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getSenderType().name(),
                message.getMessageType().name(),
                message.getContent(),
                message.getOriginalContent(),
                message.getCreatedAt()
        );
    }
}