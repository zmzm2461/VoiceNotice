package com.example.voicenotice.quickreply.dto;

import java.util.List;

public record QuickReplySuggestRequest(
        String query,
        List<QuickReplyCandidate> candidates
) {

    public record QuickReplyCandidate(
            Integer replyCode,
            String text
    ) {
    }
}