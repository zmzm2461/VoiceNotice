package com.example.voicenotice.conversation.service;

import com.example.voicenotice.conversation.dto.ConversationMessageResponse;
import com.example.voicenotice.conversation.entity.ConversationMessage;
import com.example.voicenotice.conversation.entity.MessageType;
import com.example.voicenotice.conversation.entity.SenderType;
import com.example.voicenotice.conversation.repository.ConversationMessageRepository;
import com.example.voicenotice.session.entity.IntercomSession;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private final ConversationMessageRepository conversationMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ConversationMessage saveVisitorSttMessage(
            IntercomSession session,
            String text
    ) {
        ConversationMessage saved = conversationMessageRepository.save(
                new ConversationMessage(
                        session,
                        SenderType.VISITOR,
                        MessageType.STT,
                        text
                )
        );

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/messages",
                ConversationMessageResponse.from(saved)
        );

        return saved;
    }

    @Transactional
    public ConversationMessage saveUserQuickReplyMessage(
            IntercomSession session,
            String text
    ) {
        ConversationMessage saved = conversationMessageRepository.save(
                new ConversationMessage(
                        session,
                        SenderType.USER,
                        MessageType.QUICK_REPLY,
                        text
                )
        );

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/messages",
                ConversationMessageResponse.from(saved)
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getMessages(Long sessionId) {
        return conversationMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ConversationMessage updateMessage(Long messageId, String newContent) {
        ConversationMessage message = conversationMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("대화 메시지를 찾을 수 없습니다."));

        message.updateContent(newContent);

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + message.getSession().getId() + "/messages/update",
                ConversationMessageResponse.from(message)
        );

        return message;
    }
}