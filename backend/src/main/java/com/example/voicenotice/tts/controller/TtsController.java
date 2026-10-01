package com.example.voicenotice.tts.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.service.SessionService;
import com.example.voicenotice.tts.service.TtsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class TtsController {

    private final TtsService ttsService;
    private final SessionService sessionService;
    private final ConversationMessageService conversationMessageService;


    @PostMapping("/{sessionId}/tts")
    public Mono<ResponseEntity<ApiResponse<Void>>> sendTts(
            @PathVariable Long sessionId,
            @RequestBody TtsRequest request,
            HttpServletRequest httpRequest
    ) {

        Long userId =
                (Long) httpRequest.getAttribute("userId");


        if (userId == null) {
            throw new IllegalArgumentException(
                    "로그인이 필요합니다."
            );
        }


        if (request.text() == null ||
                request.text().isBlank()) {

            throw new IllegalArgumentException(
                    "전송할 메시지를 입력해주세요."
            );
        }


        /*
         * 현재 사용자가 이 인터폰 세션에
         * 접근할 권한이 있는지 확인
         */
        sessionService.validateUserSessionAccess(
                userId,
                sessionId
        );


        /*
         * sessionId로 실제 인터폰 세션 조회
         */
        IntercomSession session =
                sessionService.getOrThrow(
                        sessionId
                );


        String text =
                request.text().trim();


        /*
         * 사용자가 입력한 텍스트를
         * 채팅 메시지로 DB에 저장
         *
         * 동시에 /topic/sessions/{sessionId}/messages
         * 로 프론트에 전송
         */
        conversationMessageService
                .saveUserTextMessage(
                        session,
                        text
                );


        /*
         * 해당 세션의 실제 ESP32 deviceUid 사용
         *
         * 프론트가 deviceId를 보내게 하지 않음
         */
        String deviceId =
                session
                        .getDevice()
                        .getDeviceUid();


        /*
         * OpenAI TTS
         * → PCM
         * → ESP32 WebSocket
         */
        return ttsService
                .sendTts(
                        deviceId,
                        text
                )
                .thenReturn(
                        ResponseEntity.ok(
                                ApiResponse.ok(null)
                        )
                );
    }


    public record TtsRequest(
            String text
    ) {}
}