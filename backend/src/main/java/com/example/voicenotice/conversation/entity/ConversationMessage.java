package com.example.voicenotice.conversation.entity;

import com.example.voicenotice.session.entity.IntercomSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_messages")
@Getter
@NoArgsConstructor
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private IntercomSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String originalContent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ConversationMessage(
            IntercomSession session,
            SenderType senderType,
            MessageType messageType,
            String content
    ) {
        this.session = session;
        this.senderType = senderType;
        this.messageType = messageType;
        this.content = content;
        this.originalContent = null;
        this.createdAt = LocalDateTime.now();
    }

    public void updateContent(String newContent) {
        if (this.originalContent == null) {
            this.originalContent = this.content;
        }
        this.content = newContent;
    }
}