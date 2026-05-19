package com.example.voicenotice.device.repository;

import com.example.voicenotice.device.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceCommandRepository
        extends JpaRepository<DeviceCommand, Long> {

    Optional<DeviceCommand>
    findTopByDevice_DeviceUidAndProcessedFalseOrderByIdAsc(
            String deviceUid
    );
}