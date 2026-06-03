package com.example.voicenotice.admin.service;

import com.example.voicenotice.admin.dto.AdminDeviceResponse;
import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDeviceService {

    private final DeviceRepository deviceRepository;

    public List<AdminDeviceResponse> getAllDevices() {
        return deviceRepository
                .findByStatusNotOrderByCreatedAtDesc("DELETED")
                .stream()
                .map(AdminDeviceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDeviceResponse getDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("디바이스를 찾을 수 없습니다."));

        return AdminDeviceResponse.from(device);
    }

    @Transactional(readOnly = true)
    public List<AdminDeviceResponse> searchDevices(
            String deviceUid,
            String status
    ) {
        if (deviceUid == null) {
            deviceUid = "";
        }

        if (status == null) {
            status = "";
        }

        return deviceRepository
                .findByDeviceUidContainingAndStatusContainingOrderByCreatedAtDesc(
                        deviceUid,
                        status
                )
                .stream()
                .map(AdminDeviceResponse::from)
                .toList();
    }

    @Transactional
    public void disableDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("디바이스를 찾을 수 없습니다."));

        device.deactivate();
    }

    @Transactional
    public void enableDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("디바이스를 찾을 수 없습니다."));

        device.activate();
    }

    @Transactional
    public void deleteDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("디바이스를 찾을 수 없습니다."));

        device.delete();
    }

}