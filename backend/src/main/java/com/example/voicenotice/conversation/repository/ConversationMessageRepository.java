package com.example.voicenotice.conversation.repository;

import com.example.voicenotice.conversation.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);
}