package com.example.voicenotice.api.notice;

import com.example.voicenotice.api.notice.dto.NoticeResponse;
import com.example.voicenotice.domain.notice.Notice;
import com.example.voicenotice.domain.notice.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notices")
public class NoticeQueryController {

    private final NoticeService noticeService;

    public NoticeQueryController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public Page<NoticeResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return noticeService.getNotices(pageable)
                .map(n -> new NoticeResponse(
                        n.getId(),
                        n.getFinalText(),
                        n.getSummary(),
                        n.getCategory(),
                        n.getCreatedAt()
                ));
    }

    @GetMapping("/{id}")
    public NoticeResponse getOne(@PathVariable Long id) {
        Notice n = noticeService.getNoticeOrThrow(id);

        return new NoticeResponse(
                n.getId(),
                n.getFinalText(),
                n.getSummary(),
                n.getCategory(),
                n.getCreatedAt()
        );
    }
}
