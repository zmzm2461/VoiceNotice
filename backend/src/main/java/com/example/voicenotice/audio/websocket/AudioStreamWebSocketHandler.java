package com.example.voicenotice.audio.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.voicenotice.stt.client.RealtimeSttClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioStreamWebSocketHandler extends AbstractWebSocketHandler {

    private final RealtimeSttClient realtimeSttClient;

    private static final String SESSION_ID_KEY = "intercomSessionId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        Long sessionId = extractSessionId(session);

        if (sessionId == null) {

            log.warn(
                    "[Realtime Audio] sessionId missing. uri={}",
                    session.getUri()
            );

            session.close(
                    CloseStatus.BAD_DATA.withReason("sessionId is required")
            );

            return;
        }

        session.getAttributes().put(
                SESSION_ID_KEY,
                sessionId
        );

        realtimeSttClient.connect(sessionId);

        log.info(
                "[Realtime Audio] WebSocket connected. sessionId={}, socketId={}",
                sessionId,
                session.getId()
        );
    }


    /*
     * ESP32가 보내는 PCM Binary 데이터
     */
    @Override
    protected void handleBinaryMessage(
            WebSocketSession session,
            BinaryMessage message
    ) {

        Long sessionId = getSessionId(session);


        if (sessionId == null) {

            log.warn(
                    "[Realtime Audio] PCM received without sessionId"
            );

            return;
        }


        byte[] pcmBytes =
                new byte[message.getPayload().remaining()];


        message.getPayload().get(
                pcmBytes
        );


        log.debug(
                "[Realtime Audio] PCM received. sessionId={}, bytes={}",
                sessionId,
                pcmBytes.length
        );


        realtimeSttClient.sendAudio(
                sessionId,
                pcmBytes
        );
    }


    /*
     * ESP32가 보내는 제어 메시지
     *
     * 예:
     * {"type":"commit"}
     */
    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        Long sessionId = getSessionId(session);


        if (sessionId == null) {

            log.warn(
                    "[Realtime Audio] Control message without sessionId"
            );

            return;
        }


        String payload =
                message.getPayload();


        log.info(
                "[Realtime Audio] Control received. sessionId={}, payload={}",
                sessionId,
                payload
        );


        realtimeSttClient.sendControl(
                sessionId,
                payload
        );
    }


    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {

        log.error(
                "[Realtime Audio] WebSocket error. socketId={}",
                session.getId(),
                exception
        );
    }


    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        Long sessionId = getSessionId(session);


        if (sessionId != null) {

            realtimeSttClient.close(
                    sessionId
            );
        }


        log.info(
                "[Realtime Audio] WebSocket closed. sessionId={}, status={}",
                sessionId,
                status
        );
    }


    private Long extractSessionId(WebSocketSession session) {

        if (session.getUri() == null) {
            return null;
        }

        String sessionId = UriComponentsBuilder
                .fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("sessionId");

        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private Long getSessionId(WebSocketSession session) {

        Object value = session
                .getAttributes()
                .get(SESSION_ID_KEY);

        if (value instanceof Long sessionId) {
            return sessionId;
        }

        return null;
    }
}