package com.example.voicenotice.session.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import com.example.voicenotice.device.entity.Device;
import com.example.voicenotice.device.entity.DevicePairing;
import com.example.voicenotice.device.repository.DevicePairingRepository;
import com.example.voicenotice.device.service.DeviceCommandService;
import com.example.voicenotice.device.service.DeviceService;
import com.example.voicenotice.intercomlog.service.IntercomLogService;
import com.example.voicenotice.notification.entity.PushToken;
import com.example.voicenotice.notification.repository.PushTokenRepository;
import com.example.voicenotice.notification.service.PushNotificationService;
import com.example.voicenotice.quickreply.entity.QuickReply;
import com.example.voicenotice.quickreply.entity.QuickReplyUsage;
import com.example.voicenotice.quickreply.repository.QuickReplyUsageRepository;
import com.example.voicenotice.quickreply.service.QuickReplyService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.entity.SessionStatus;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import com.example.voicenotice.stt.dto.CallStatusMessage;
import com.example.voicenotice.stt.dto.ReplyMessage;
import com.example.voicenotice.transcript.repository.FinalTranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final IntercomSessionRepository sessionRepository;
    private final QuickReplyUsageRepository quickReplyUsageRepository;
    private final DeviceService deviceService;
    private final PushNotificationService pushNotificationService;
    private final PushTokenRepository pushTokenRepository;
    private final DevicePairingRepository devicePairingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QuickReplyService quickReplyService;
    private final DeviceCommandService deviceCommandService;
    private final ConversationMessageService conversationMessageService;
    private final FinalTranscriptRepository finalTranscriptRepository;
    private final IntercomLogService intercomLogService;

    @Transactional
    public IntercomSession start(String deviceUid) {
        Device device = deviceService.getByUid(deviceUid);
        device.heartbeat();

        IntercomSession session = sessionRepository.save(new IntercomSession(device));

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/status",
                new CallStatusMessage(
                        session.getId(),
                        "CALLING",
                        "방문자가 인터폰을 호출했습니다."
                )
        );

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

        if (session.getStatus() == SessionStatus.CLOSED) {
            return session;
        }

        session.close();

        finalTranscriptRepository.findBySession_Id(sessionId)
                .ifPresent(intercomLogService::createIfNotExists);

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/status",
                new CallStatusMessage(
                        session.getId(),
                        "ENDED",
                        "통화가 종료되었습니다."
                )
        );

        messagingTemplate.convertAndSend(
                "/topic/admin/monitoring",
                new CallStatusMessage(
                        session.getId(),
                        "ENDED",
                        "관리자 모니터링에서 제거할 세션입니다."
                )
        );

        return session;
    }

    @Transactional
    public IntercomSession connect(Long sessionId) {
        IntercomSession session = getOrThrow(sessionId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 채팅방입니다.");
        }

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/status",
                new CallStatusMessage(
                        session.getId(),
                        "TALKING",
                        "통화가 연결되었습니다."
                )
        );

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

    @Transactional
    public void sendReply(Long userId, Long sessionId, Integer replyCode) {
        IntercomSession session = getOrThrow(sessionId);

        devicePairingRepository
                .findByDevice_DeviceUidAndUser_IdAndUnpairedAtIsNull(
                        session.getDevice().getDeviceUid(),
                        userId
                )
                .orElseThrow(() -> new IllegalArgumentException("해당 기기에 대한 권한이 없습니다."));

        QuickReply quickReply = quickReplyService.getByReplyCode(replyCode);

        quickReplyUsageRepository.save(
                new QuickReplyUsage(
                        quickReply.getReplyCode(),
                        quickReply.getText(),
                        sessionId,
                        userId
                )
        );

        conversationMessageService.saveUserQuickReplyMessage(
                session,
                quickReply.getText()
        );

        deviceCommandService.createPlayReplyCommand(
                session.getDevice(),
                replyCode
        );

        messagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/messages",
                new ReplyMessage(
                        session.getId(),
                        "USER",
                        quickReply.getText()
                )
        );
    }

    @Transactional(readOnly = true)
    public void validateUserSessionAccess(Long userId, Long sessionId) {
        IntercomSession session = getOrThrow(sessionId);

        devicePairingRepository
                .findByDevice_DeviceUidAndUser_IdAndUnpairedAtIsNull(
                        session.getDevice().getDeviceUid(),
                        userId
                )
                .orElseThrow(() -> new IllegalArgumentException("해당 세션에 대한 권한이 없습니다."));
    }
}