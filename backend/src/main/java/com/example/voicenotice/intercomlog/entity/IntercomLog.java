package com.example.voicenotice.intercomlog.entity;

import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "intercom_logs")
@Getter
@NoArgsConstructor
public class IntercomLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intercom_log_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_transcript_id", nullable = false, unique = true)
    private FinalTranscript finalTranscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "visitor_text", columnDefinition = "TEXT")
    private String visitorText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 30)
    private String intent;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IntercomLog(
            FinalTranscript finalTranscript,
            Device device,
            Long sessionId,
            String visitorText,
            String summary,
            String intent,
            String status
    ) {
        this.finalTranscript = finalTranscript;
        this.device = device;
        this.sessionId = sessionId;
        this.visitorText = visitorText;
        this.summary = summary;
        this.intent = intent;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}