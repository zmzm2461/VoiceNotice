package com.example.voicenotice.session.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.entity.DevicePairing;
import com.example.voicenotice.device.repository.DevicePairingRepository;
import com.example.voicenotice.device.service.DeviceService;
import com.example.voicenotice.notification.entity.PushToken;
import com.example.voicenotice.notification.repository.PushTokenRepository;
import com.example.voicenotice.notification.service.PushNotificationService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final IntercomSessionRepository sessionRepository;
    private final DeviceService deviceService;
    private final PushNotificationService pushNotificationService;
    private final PushTokenRepository pushTokenRepository;
    private final DevicePairingRepository devicePairingRepository;


    @Transactional
    public IntercomSession start(String deviceUid) {
        Device device = deviceService.getByUid(deviceUid);
        device.heartbeat();

        IntercomSession session = sessionRepository.save(new IntercomSession(device));

        List<DevicePairing> pairings =
                devicePairingRepository.findByDevice_DeviceUidAndUnpairedAtIsNull(deviceUid);

        for (DevicePairing pairing : pairings) {
            List<PushToken> tokens =
                    pushTokenRepository.findByUser_Id(pairing.getUser().getId());

            for (PushToken pushToken : tokens) {
                pushNotificationService.sendToToken(
                        pushToken.getToken(),
                        "인터폰 호출",
                        "방문자가 인터폰을 호출했습니다."
                );
            }
        }

        return session;
    }

    @Transactional(readOnly = true)
    public IntercomSession getOrThrow(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    }

    @Transactional
    public IntercomSession close(Long sessionId) {
        IntercomSession session = getOrThrow(sessionId);
        session.close();
        return session;
    }

    @Transactional(readOnly = true)
    public List<IntercomSession> getByDeviceUid(String deviceUid) {
        return sessionRepository.findByDevice_DeviceUidOrderByStartedAtDesc(deviceUid);
    }

    @Transactional(readOnly = true)
    public Optional<IntercomSession> getCurrentByDeviceUid(String deviceUid) {
        return sessionRepository.findTopByDevice_DeviceUidAndStatusOrderByStartedAtDesc(
                deviceUid,
                SessionStatus.OPEN
        );
    }

}
