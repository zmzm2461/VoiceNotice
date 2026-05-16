package com.example.voicenotice.audio.entity;

import com.example.voicenotice.session.entity.IntercomSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_chunks", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "chunk_order"}))
@Getter
@NoArgsConstructor
public class AudioChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private IntercomSession session;

    @Column(name = "chunk_order", nullable = false)
    private Integer chunkOrder;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AudioChunk(IntercomSession session, Integer chunkOrder, String fileName, String filePath, Integer durationMs) {
        this.session = session;
        this.chunkOrder = chunkOrder;
        this.fileName = fileName;
        this.filePath = filePath;
        this.durationMs = durationMs;
        this.uploadedAt = LocalDateTime.now();
        this.createdAt = this.uploadedAt;
    }

    public void updateChunkFile(String fileName, String filePath, Integer durationMs) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.durationMs = durationMs;
        this.uploadedAt = LocalDateTime.now();
    }
}
