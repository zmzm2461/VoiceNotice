package com.example.voicenotice.notice.controller;

import com.example.voicenotice.notice.dto.NoticeResponse;
import com.example.voicenotice.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    // 전체 공지 조회
    @GetMapping
    public List<NoticeResponse> getAll() {
        return noticeService.getAll();
    }

    // 공지 상세 조회
    @GetMapping("/{id}")
    public NoticeResponse get(@PathVariable Long id) {
        return noticeService.get(id);
    }
}