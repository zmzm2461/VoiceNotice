package com.example.voicenotice.transcript.entity;

import com.example.voicenotice.session.entity.IntercomSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "final_transcripts")
@Getter
@NoArgsConstructor
public class FinalTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "final_transcript_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private IntercomSession session;

    @Column(name = "merged_text", columnDefinition = "TEXT")
    private String mergedText;

    @Column(name = "refined_text", columnDefinition = "TEXT")
    private String refinedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinalTranscriptStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private String category;

    public FinalTranscript(IntercomSession session, String mergedText) {
        this.session = session;
        this.mergedText = mergedText;
        this.status = FinalTranscriptStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void succeed(String refinedText) {
        this.refinedText = refinedText;
        this.status = FinalTranscriptStatus.SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = FinalTranscriptStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCategory(String category) {
        this.category = category;
    }
}
