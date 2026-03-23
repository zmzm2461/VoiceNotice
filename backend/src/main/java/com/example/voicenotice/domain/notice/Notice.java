package com.example.voicenotice.domain.notice;

import com.example.voicenotice.domain.transcript.Transcript;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@Setter
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcript_id", nullable = false)
    private Transcript transcript;

    @Lob
    @Column(name="final_text", nullable = false)
    private String finalText;

    @Column(name="summary", length = 255)
    private String summary;

    @Column(name="category", length = 50)
    private String category;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notice() { }

    public Notice(Transcript transcript, String finalText, String summary, String category) {
        this.transcript = transcript;
        this.finalText = finalText;
        this.summary = summary;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }

}
