package com.example.voicenotice.session.entity;

import com.example.voicenotice.device.entity.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "intercom_sessions")
@Getter
@NoArgsConstructor
public class IntercomSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public IntercomSession(Device device) {
        this.device = device;
        this.status = SessionStatus.OPEN;
        this.startedAt = LocalDateTime.now();
        this.createdAt = this.startedAt;
        this.updatedAt = this.startedAt;
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
        this.endedAt = LocalDateTime.now();
        this.updatedAt = this.endedAt;
    }
}
