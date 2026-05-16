package com.example.voicenotice.notice.service;

import com.example.voicenotice.notice.dto.NoticeResponse;
import com.example.voicenotice.notice.entity.Notice;
import com.example.voicenotice.notice.repository.NoticeRepository;
import com.example.voicenotice.notification.service.PushNotificationService;
import com.example.voicenotice.stt.client.TextRefinerClient;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final TextRefinerClient textRefinerClient;
    private final PushNotificationService pushNotificationService;

    public Notice createIfNotExists(FinalTranscript finalTranscript) {
        return noticeRepository.findByFinalTranscript_Id(finalTranscript.getId())
                .orElseGet(() -> {
                    String category = finalTranscript.getCategory();
                    String finalText = finalTranscript.getRefinedText();

                    boolean emergency = "EMERGENCY".equals(category);

                    String summary = textRefinerClient.summarize(finalText);

                    Notice notice = new Notice(
                            finalTranscript,
                            finalTranscript.getSession().getDevice(),
                            category,
                            emergency,
                            finalText,
                            summary
                    );

                    Notice saved = noticeRepository.save(notice);

                    pushNotificationService.sendToAll(
                            "[" + category + "] 새로운 공지",
                            summary
                    );

                    return saved;
                });
    }

    private String makeSimpleSummary(String text) {
        if (text == null || text.isBlank()) {
            return "내용 없음";
        }

        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }

    public List<NoticeResponse> getAll() {
        return noticeRepository.findAll()
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    public NoticeResponse get(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지 없음"));

        return NoticeResponse.from(notice);
    }

}