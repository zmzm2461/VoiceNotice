package com.example.voicenotice.quickreply.service;

import com.example.voicenotice.common.exception.NotFoundException;
import com.example.voicenotice.quickreply.dto.QuickReplyResponse;
import com.example.voicenotice.quickreply.entity.QuickReply;
import com.example.voicenotice.quickreply.repository.QuickReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuickReplyService {

    private final QuickReplyRepository quickReplyRepository;

    @Transactional(readOnly = true)
    public List<QuickReplyResponse> getReplies() {
        return quickReplyRepository.findByActiveTrueOrderByReplyCodeAsc()
                .stream()
                .map(QuickReplyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuickReply getByReplyCode(Integer replyCode) {
        return quickReplyRepository.findByReplyCodeAndActiveTrue(replyCode)
                .orElseThrow(() ->
                        new NotFoundException("Quick reply not found")
                );
    }
}