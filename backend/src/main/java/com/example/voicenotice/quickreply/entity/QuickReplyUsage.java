package com.example.voicenotice.quickreply.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quick_reply_usages")
@Getter
@NoArgsConstructor
public class QuickReplyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reply_code", nullable = false)
    private Integer replyCode;

    @Column(name = "reply_text", nullable = false, columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    public QuickReplyUsage(Integer replyCode, String replyText, Long sessionId, Long userId) {
        this.replyCode = replyCode;
        this.replyText = replyText;
        this.sessionId = sessionId;
        this.userId = userId;
        this.usedAt = LocalDateTime.now();
    }
}