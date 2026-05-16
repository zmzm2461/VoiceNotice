package com.example.voicenotice.device.service;

import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.entity.DevicePairing;
import com.example.voicenotice.device.repository.DevicePairingRepository;
import com.example.voicenotice.user.entity.User;
import com.example.voicenotice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DevicePairingService {

    private final DevicePairingRepository devicePairingRepository;
    private final DeviceService deviceService;
    private final UserService userService;

    @Transactional
    public DevicePairing pair(String deviceUid, Long userId) {
        Device device = deviceService.getByUid(deviceUid);
        User user = userService.getById(userId);

        return devicePairingRepository
                .findByDevice_DeviceUidAndUser_IdAndUnpairedAtIsNull(deviceUid, userId)
                .orElseGet(() -> devicePairingRepository.save(new DevicePairing(device, user)));
    }
}