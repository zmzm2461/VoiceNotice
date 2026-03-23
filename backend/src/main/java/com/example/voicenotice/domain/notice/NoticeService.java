package com.example.voicenotice.domain.notice;

import com.example.voicenotice.domain.transcript.Transcript;
import com.example.voicenotice.domain.transcript.TranscriptService;
import com.example.voicenotice.infra.ai.TextRefinerClient;
import com.example.voicenotice.infra.ai.dto.RefineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoticeService {

    private final TranscriptService transcriptService;
    private final NoticeRepository noticeRepository;
    private final TextRefinerClient textRefinerClient;

    public NoticeService(TranscriptService transcriptService,
                         NoticeRepository noticeRepository,
                         TextRefinerClient textRefinerClient) {
        this.transcriptService = transcriptService;
        this.noticeRepository = noticeRepository;
        this.textRefinerClient = textRefinerClient;
    }

    @Transactional
    public Long createFromTranscript(Long transcriptId) {
        Transcript t = transcriptService.getOrThrow(transcriptId);

        RefineResponse refined = textRefinerClient.refine(t.getRawText());
        if (refined == null || refined.getFinalText() == null || refined.getFinalText().isBlank()) {
            throw new IllegalStateException("AI refine failed: empty finalText");
        }

        Notice notice = new Notice(
                t,
                refined.getFinalText(),
                refined.getSummary(),
                refined.getCategory()
        );

        noticeRepository.save(notice);
        return notice.getId();
    }


    public Page<Notice> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }


    public Notice getNoticeOrThrow(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
    }
}
