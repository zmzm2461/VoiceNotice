package com.example.voicenotice.device.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public Device register(String deviceUid, String location) {
        return deviceRepository.findByDeviceUid(deviceUid)
                .map(device -> {
                    device.heartbeat();
                    return device;
                })
                .orElseGet(() -> deviceRepository.save(new Device(deviceUid, location)));
    }

    @Transactional(readOnly = true)
    public Device getByUid(String deviceUid) {
        return deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceUid));
    }
}
