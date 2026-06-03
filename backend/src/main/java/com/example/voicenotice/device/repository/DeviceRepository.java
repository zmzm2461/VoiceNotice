package com.example.voicenotice.device.repository;

import com.example.voicenotice.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceUid(String deviceUid);

    boolean existsByDeviceUid(String deviceUid);

    List<Device> findByDeviceUidContainingAndStatusContainingOrderByCreatedAtDesc(
            String deviceUid,
            String status
    );

    List<Device> findByStatusNotOrderByCreatedAtDesc(String status);
}
