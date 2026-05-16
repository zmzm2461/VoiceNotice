package com.example.voicenotice.transcript.entity;

import com.example.voicenotice.session.entity.IntercomSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcript_chunks", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "chunk_order"}))
@Getter
@NoArgsConstructor
public class TranscriptChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_chunk_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private IntercomSession session;

    @Column(name = "chunk_order", nullable = false)
    private Integer chunkOrder;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TranscriptChunk(IntercomSession session, Integer chunkOrder, String rawText, BigDecimal confidence) {
        this.session = session;
        this.chunkOrder = chunkOrder;
        this.rawText = rawText;
        this.confidence = confidence;
        this.createdAt = LocalDateTime.now();
    }
}
