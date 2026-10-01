package com.example.voicenotice.audio.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class TtsAudioWebSocketHandler extends TextWebSocketHandler {

    /*
     * deviceId별 ESP32 WebSocket 연결 저장
     *
     * 예:
     * ESP32-001 -> WebSocketSession
     */
    private final Map<String, WebSocketSession> deviceSessions =
            new ConcurrentHashMap<>();


    /*
     * ESP32가
     *
     * ws://서버/tts/audio?deviceId=ESP32-001
     *
     * 로 접속하면 실행됨
     */
    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) throws Exception {

        String deviceId = getDeviceId(session);

        if (deviceId == null || deviceId.isBlank()) {

            System.out.println(
                    "[TTS SOCKET] deviceId 없음"
            );

            session.close(
                    CloseStatus.BAD_DATA
            );

            return;
        }


        /*
         * 동시에 여러 PCM 조각을 sendMessage할 때
         * WebSocketSession 충돌을 막기 위한 wrapper
         */
        WebSocketSession safeSession =
                new ConcurrentWebSocketSessionDecorator(
                        session,
                        5000,
                        1024 * 1024
                );


        deviceSessions.put(
                deviceId,
                safeSession
        );


        System.out.println(
                "[TTS SOCKET CONNECTED]"
                        + " deviceId=" + deviceId
                        + ", socketId=" + session.getId()
        );
    }


    /*
     * TTS 시작 신호
     *
     * ESP32는 이 메시지를 받으면
     * 마이크 → STT 전송을 중단하면 됨
     */
    public void sendStart(
            String deviceId
    ) {

        sendText(
                deviceId,
                "{\"type\":\"tts_start\",\"sampleRate\":24000}"
        );


        System.out.println(
                "[TTS SOCKET] start"
                        + " deviceId=" + deviceId
        );
    }


    /*
     * 실제 PCM 데이터를 ESP32로 전송
     */
    public void sendAudio(
            String deviceId,
            byte[] pcm
    ) {

        WebSocketSession session =
                deviceSessions.get(deviceId);


        if (session == null || !session.isOpen()) {

            throw new IllegalStateException(
                    "TTS WebSocket이 연결되어 있지 않습니다. deviceId="
                            + deviceId
            );
        }


        try {

            session.sendMessage(
                    new BinaryMessage(pcm)
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "ESP32 TTS PCM 전송 실패. deviceId="
                            + deviceId,
                    e
            );
        }
    }


    /*
     * TTS 종료 신호
     *
     * ESP32는 버퍼의 남은 음성을 전부 재생한 뒤
     * 다시 마이크 STT 전송을 활성화하면 됨
     */
    public void sendEnd(
            String deviceId
    ) {

        sendText(
                deviceId,
                "{\"type\":\"tts_end\"}"
        );


        System.out.println(
                "[TTS SOCKET] end"
                        + " deviceId=" + deviceId
        );
    }


    /*
     * Text WebSocket 메시지 전송 공통 함수
     */
    private void sendText(
            String deviceId,
            String message
    ) {

        WebSocketSession session =
                deviceSessions.get(deviceId);


        if (session == null || !session.isOpen()) {

            throw new IllegalStateException(
                    "TTS WebSocket이 연결되어 있지 않습니다. deviceId="
                            + deviceId
            );
        }


        try {

            session.sendMessage(
                    new TextMessage(message)
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "TTS WebSocket 메시지 전송 실패. deviceId="
                            + deviceId,
                    e
            );
        }
    }


    /*
     * ESP32 연결 종료
     */
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String deviceId =
                getDeviceId(session);


        if (deviceId != null) {

            deviceSessions.remove(
                    deviceId
            );
        }


        System.out.println(
                "[TTS SOCKET CLOSED]"
                        + " deviceId=" + deviceId
                        + ", status=" + status
        );
    }


    /*
     * URL에서 deviceId 추출
     *
     * /tts/audio?deviceId=ESP32-001
     */
    private String getDeviceId(
            WebSocketSession session
    ) {

        URI uri = session.getUri();

        if (uri == null ||
                uri.getQuery() == null) {

            return null;
        }


        String[] params =
                uri.getQuery().split("&");


        for (String param : params) {

            String[] pair =
                    param.split("=", 2);

            if (
                    pair.length == 2
                            && "deviceId".equals(pair[0])
            ) {

                return pair[1];
            }
        }


        return null;
    }
}