package com.example.voicenotice.domain.transcript;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcripts")
@Getter
@Setter
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranscriptStatus status;

    @Column(length = 500)
    private String errorMessage;

    @Column(name = "device_uid", nullable = false)
    private String deviceUid;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Transcript() {}

    public Transcript(String deviceUid, String rawText, LocalDateTime recordedAt) {
        this.deviceUid = deviceUid;
        this.rawText = rawText;
        this.recordedAt = recordedAt;
        this.createdAt = LocalDateTime.now();
        this.status = TranscriptStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = TranscriptStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = TranscriptStatus.FAILED;
        this.errorMessage = message;
    }

}
