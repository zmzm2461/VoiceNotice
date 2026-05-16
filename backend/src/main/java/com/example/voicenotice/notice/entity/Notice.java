package com.example.voicenotice.notice.entity;

import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_id", nullable = false, unique = true)
    private FinalTranscript finalTranscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "is_emergency", nullable = false)
    private boolean emergency;

    @Column(name = "final_text", columnDefinition = "TEXT")
    private String finalText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notice(
            FinalTranscript finalTranscript,
            Device device,
            String category,
            boolean emergency,
            String finalText,
            String summary
    ) {
        this.finalTranscript = finalTranscript;
        this.device = device;
        this.category = category;
        this.emergency = emergency;
        this.finalText = finalText;
        this.summary = summary;
        this.createdAt = LocalDateTime.now();
    }
}