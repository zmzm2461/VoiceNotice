package com.example.voicenotice.stt.service;

import com.example.voicenotice.stt.dto.CallStatusMessage;
import com.example.voicenotice.stt.dto.ReplyMessage;
import com.example.voicenotice.stt.dto.TranscriptMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WebSocketSenderService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendTranscript(
            Long sessionId,
            Integer chunkOrder,
            String rawText,
            BigDecimal confidence
    ) {
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/transcripts",
                new TranscriptMessage(sessionId, chunkOrder, rawText, confidence)
        );
    }

    public void sendStatus(Long sessionId, String status, String message) {
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/status",
                new CallStatusMessage(sessionId, status, message)
        );
    }

    public void sendReply(Long sessionId, String sender, String text) {
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/messages",
                new ReplyMessage(sessionId, sender, text)
        );
    }
}