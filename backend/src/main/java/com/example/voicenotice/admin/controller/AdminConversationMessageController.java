package com.example.voicenotice.admin.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.conversation.dto.ConversationMessageResponse;
import com.example.voicenotice.conversation.dto.UpdateConversationMessageRequest;
import com.example.voicenotice.conversation.entity.ConversationMessage;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/conversation-messages")
public class AdminConversationMessageController {

    private final ConversationMessageService conversationMessageService;

    @PatchMapping("/{messageId}")
    public ApiResponse<ConversationMessageResponse> updateMessage(
            @PathVariable Long messageId,
            @RequestBody UpdateConversationMessageRequest request
    ) {
        ConversationMessage updated =
                conversationMessageService.updateMessage(messageId, request.getContent());

        return ApiResponse.ok(ConversationMessageResponse.from(updated));
    }
}