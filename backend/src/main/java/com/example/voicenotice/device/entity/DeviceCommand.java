package com.example.voicenotice.device.entity;

import com.example.voicenotice.device.entity.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private String commandType;

    @Column(nullable = false)
    private Integer replyCode;

    @Column(nullable = false)
    private Boolean processed = false;

    public DeviceCommand(
            Device device,
            String commandType,
            Integer replyCode
    ) {
        this.device = device;
        this.commandType = commandType;
        this.replyCode = replyCode;
        this.processed = false;
    }

    public void processed() {
        this.processed = true;
    }
}