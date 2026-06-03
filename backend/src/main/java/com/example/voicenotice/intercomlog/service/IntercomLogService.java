package com.example.voicenotice.intercomlog.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.device.entity.DevicePairing;
import com.example.voicenotice.device.repository.DevicePairingRepository;
import com.example.voicenotice.intercomlog.dto.IntercomLogResponse;
import com.example.voicenotice.intercomlog.entity.IntercomLog;
import com.example.voicenotice.intercomlog.repository.IntercomLogRepository;
import com.example.voicenotice.notification.service.PushNotificationService;
import com.example.voicenotice.stt.client.TextRefinerClient;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntercomLogService {

    private final IntercomLogRepository intercomLogRepository;
    private final DevicePairingRepository devicePairingRepository;
    private final TextRefinerClient textRefinerClient;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public IntercomLog createIfNotExists(FinalTranscript finalTranscript) {
        return intercomLogRepository.findByFinalTranscript_Id(finalTranscript.getId())
                .orElseGet(() -> {
                    String finalText = finalTranscript.getRefinedText();

                    if (finalText == null || finalText.isBlank()) {
                        finalText = finalTranscript.getMergedText();
                    }

                    String summary = summarizeSafely(finalText);
                    String intent = classifyIntent(finalText);

                    IntercomLog log = new IntercomLog(
                            finalTranscript,
                            finalTranscript.getSession().getDevice(),
                            finalTranscript.getSession().getId(),
                            finalText,
                            summary,
                            intent,
                            finalTranscript.getStatus().name()
                    );

                    IntercomLog saved = intercomLogRepository.save(log);

                    pushNotificationService.sendToAll(
                            "방문자 음성이 감지되었습니다",
                            summary
                    );

                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public List<IntercomLogResponse> getAll() {
        return intercomLogRepository.findAll()
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntercomLogResponse get(Long id) {
        IntercomLog log = intercomLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("인터폰 대화 기록 없음"));

        return IntercomLogResponse.from(log);
    }

    private String summarizeSafely(String text) {
        try {
            return textRefinerClient.summarize(text);
        } catch (Exception e) {
            if (text == null || text.isBlank()) {
                return "내용 없음";
            }
            return text.length() > 30 ? text.substring(0, 30) + "..." : text;
        }
    }

    @Transactional(readOnly = true)
    public List<IntercomLogResponse> searchMyLogs(Long userId, String keyword) {
        List<Long> deviceIds = getMyDeviceIds(userId);

        if (deviceIds.isEmpty()) {
            return List.of();
        }

        if (keyword == null || keyword.isBlank()) {
            return getMyLogs(userId);
        }

        return intercomLogRepository
                .findByDevice_IdInAndVisitorTextContainingOrDevice_IdInAndSummaryContainingOrderByCreatedAtDesc(
                        deviceIds,
                        keyword,
                        deviceIds,
                        keyword
                )
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntercomLogResponse> getLogs() {
        return intercomLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntercomLogResponse> getMyLogs(Long userId) {
        List<Long> deviceIds = getMyDeviceIds(userId);

        if (deviceIds.isEmpty()) {
            return List.of();
        }

        return intercomLogRepository.findByDevice_IdInOrderByCreatedAtDesc(deviceIds)
                .stream()
                .map(IntercomLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntercomLogResponse getMyLog(Long userId, Long logId) {
        List<Long> deviceIds = getMyDeviceIds(userId);

        IntercomLog log = intercomLogRepository.findByIdAndDevice_IdIn(logId, deviceIds)
                .orElseThrow(() -> new NotFoundException("인터폰 대화 기록 없음"));

        return IntercomLogResponse.from(log);
    }

    private List<Long> getMyDeviceIds(Long userId) {
        return devicePairingRepository.findByUser_IdAndUnpairedAtIsNull(userId)
                .stream()
                .map(pairing -> pairing.getDevice().getId())
                .toList();
    }

    @Transactional
    public void deleteMyLog(Long userId, Long logId) {
        List<Long> deviceIds = getMyDeviceIds(userId);

        IntercomLog log = intercomLogRepository.findByIdAndDevice_IdIn(logId, deviceIds)
                .orElseThrow(() -> new NotFoundException("삭제할 인터폰 기록이 없습니다."));

        intercomLogRepository.delete(log);
    }

    private String classifyIntent(String text) {
        if (text == null) {
            return "UNKNOWN";
        }

        if (text.contains("택배") || text.contains("배달")) {
            return "DELIVERY";
        }

        if (text.contains("방문") || text.contains("왔습니다")) {
            return "VISITOR";
        }

        if (text.contains("관리실") || text.contains("점검")) {
            return "MANAGEMENT";
        }

        return "GENERAL";
    }
}