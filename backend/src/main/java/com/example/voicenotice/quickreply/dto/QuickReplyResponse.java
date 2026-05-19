package com.example.voicenotice.quickreply.dto;

import com.example.voicenotice.quickreply.entity.QuickReply;

public record QuickReplyResponse(
        Long id,
        Integer replyCode,
        String text
) {
    public static QuickReplyResponse from(QuickReply quickReply) {
        return new QuickReplyResponse(
                quickReply.getId(),
                quickReply.getReplyCode(),
                quickReply.getText()
        );
    }
}