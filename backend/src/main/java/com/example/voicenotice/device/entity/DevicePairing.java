package com.example.voicenotice.device.entity;

import com.example.voicenotice.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_pairings")
@Getter
@NoArgsConstructor
public class DevicePairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "paired_at", nullable = false)
    private LocalDateTime pairedAt;

    @Column(name = "unpaired_at")
    private LocalDateTime unpairedAt;

    public DevicePairing(Device device, User user) {
        this.device = device;
        this.user = user;
        this.pairedAt = LocalDateTime.now();
    }

    public void unpair() {
        this.unpairedAt = LocalDateTime.now();
    }
}