package com.example.voicenotice.transcript.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcript_edit_histories")
@Getter
@NoArgsConstructor
public class TranscriptEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edit_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_chunk_id", nullable = false)
    private TranscriptChunk transcriptChunk;

    @Column(name = "before_text", columnDefinition = "TEXT")
    private String beforeText;

    @Column(name = "after_text", columnDefinition = "TEXT")
    private String afterText;

    @Column(name = "edited_by")
    private Long editedBy;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    public TranscriptEditHistory(
            TranscriptChunk transcriptChunk,
            String beforeText,
            String afterText,
            Long editedBy
    ) {
        this.transcriptChunk = transcriptChunk;
        this.beforeText = beforeText;
        this.afterText = afterText;
        this.editedBy = editedBy;
        this.editedAt = LocalDateTime.now();
    }
}