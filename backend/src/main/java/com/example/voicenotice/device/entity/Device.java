package com.example.voicenotice.device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @Column(name = "device_uid", nullable = false, unique = true, length = 100)
    private String deviceUid;

    @Column(length = 100)
    private String location;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Device(String deviceUid, String location) {
        this.deviceUid = deviceUid;
        this.location = location;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.lastSeenAt = this.createdAt;
    }

    public void heartbeat() {
        this.lastSeenAt = LocalDateTime.now();
        this.updatedAt = this.lastSeenAt;
    }
}
