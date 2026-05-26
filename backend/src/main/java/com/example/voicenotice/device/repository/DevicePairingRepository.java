package com.example.voicenotice.device.repository;

import com.example.voicenotice.device.entity.DevicePairing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevicePairingRepository extends JpaRepository<DevicePairing, Long> {

    Optional<DevicePairing> findByDevice_DeviceUidAndUser_IdAndUnpairedAtIsNull(
            String deviceUid,
            Long userId
    );

    List<DevicePairing> findByUser_IdAndUnpairedAtIsNull(Long userId);

    List<DevicePairing> findByDevice_DeviceUidAndUnpairedAtIsNull(String deviceUid);

}