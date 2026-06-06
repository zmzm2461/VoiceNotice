package com.example.voicenotice.conversation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateConversationMessageRequest {
    private String content;
}