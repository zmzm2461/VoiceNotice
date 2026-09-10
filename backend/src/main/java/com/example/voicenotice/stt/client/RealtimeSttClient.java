package com.example.voicenotice.stt.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import com.example.voicenotice.stt.service.SttOrchestrationService;
import com.example.voicenotice.stt.service.WebSocketSenderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSttClient {

    /*
     * 인터폰 세션별로
     * Spring -> Python WebSocket 연결을 보관한다.
     *
     * 예:
     * 13 -> Python WebSocket 연결
     * 14 -> Python WebSocket 연결
     */
    private final Map<Long, WebSocketSession> sessions =
            new ConcurrentHashMap<>();


    private final WebSocketClient webSocketClient =
            new StandardWebSocketClient();

    private final ObjectMapper objectMapper;

    private final WebSocketSenderService webSocketSenderService;

    private final SttOrchestrationService sttOrchestrationService;


    /*
     * Realtime STT partial은 delta 형태로 들어오기 때문에
     * 세션별로 문장을 누적한다.
     */
    private final Map<Long, StringBuilder> partialBuffers =
            new ConcurrentHashMap<>();


    @Value("${app.ai.realtime-url:ws://localhost:8000/ws/stt}")
    private String realtimeUrl;


    /*
     * ==============================
     * Python Realtime STT 연결
     * ==============================
     */
    public void connect(Long sessionId) {

        WebSocketSession existing = sessions.get(sessionId);

        /*
         * 이미 연결되어 있다면
         * 새 연결을 만들지 않는다.
         */
        if (existing != null && existing.isOpen()) {

            log.info(
                    "[Realtime STT Client] Already connected. sessionId={}",
                    sessionId
            );

            return;
        }


        try {

            WebSocketSession rawSession = webSocketClient.execute(

                    new AbstractWebSocketHandler() {

                        /*
                         * Python WebSocket 연결 성공
                         */
                        @Override
                        public void afterConnectionEstablished(
                                WebSocketSession session
                        ) {

                            log.info(
                                    "[Realtime STT Client] Python connected. sessionId={}, socketId={}",
                                    sessionId,
                                    session.getId()
                            );
                        }


                        /*
                         * Python이 보내는 JSON 메시지
                         *
                         * 현재 단계에서는 로그만 찍는다.
                         *
                         * ④단계에서:
                         * partial -> 사용자 화면
                         *
                         * ⑤단계에서:
                         * final -> DB
                         */
                        @Override
                        protected void handleTextMessage(
                                WebSocketSession session,
                                TextMessage message
                        ) {

                            String payload = message.getPayload();

                            log.info(
                                    "[Realtime STT <- Python] sessionId={}, message={}",
                                    sessionId,
                                    payload
                            );


                            try {

                                JsonNode json =
                                        objectMapper.readTree(payload);


                                String type =
                                        json.path("type").asText();


                                /*
                                 * =============================
                                 * PARTIAL
                                 * =============================
                                 */
                                if ("partial".equals(type)) {

                                    String delta =
                                            json.path("text").asText("");


                                    if (delta.isEmpty()) {
                                        return;
                                    }


                                    /*
                                     * Python에서 오는 partial은
                                     * 전체 문장이 아니라 delta이므로 누적
                                     */
                                    StringBuilder buffer =
                                            partialBuffers.computeIfAbsent(
                                                    sessionId,
                                                    key -> new StringBuilder()
                                            );


                                    String accumulatedText;


                                    synchronized (buffer) {

                                        buffer.append(delta);

                                        accumulatedText =
                                                buffer.toString();
                                    }


                                    /*
                                     * 사용자 WebSocket으로 전송
                                     *
                                     * DB 저장 X
                                     * GPT 후처리 X
                                     */
                                    webSocketSenderService.sendRealtimeTranscript(
                                            sessionId,
                                            "partial",
                                            accumulatedText
                                    );


                                    log.info(
                                            "[Realtime PARTIAL -> Client] sessionId={}, text={}",
                                            sessionId,
                                            accumulatedText
                                    );

                                    return;
                                }


                                /*
                                 * =============================
                                 * FINAL
                                 * =============================
                                 *
                                 * 아직 ⑤단계가 아니므로
                                 * DB에는 저장하지 않는다.
                                 */
                                if ("final".equals(type)) {

                                    String finalText =
                                            json.path("text").asText("").trim();


                                    /*
                                     * 다음 발화를 위해 partial 초기화
                                     */
                                    partialBuffers.remove(sessionId);


                                    if (finalText.isBlank()) {

                                        log.info(
                                                "[Realtime FINAL] Empty result skipped. sessionId={}",
                                                sessionId
                                        );

                                        return;
                                    }


                                    log.info(
                                            "[Realtime FINAL received] sessionId={}, text={}",
                                            sessionId,
                                            finalText
                                    );


                                    /*
                                     * =============================
                                     * FINAL STT → DB 저장
                                     * =============================
                                     *
                                     * GPT 후처리는 아직 하지 않는다.
                                     */
                                    sttOrchestrationService.saveRealtimeFinal(
                                            sessionId,
                                            finalText
                                    );


                                    return;
                                }


                                /*
                                 * =============================
                                 * READY
                                 * =============================
                                 */
                                if ("ready".equals(type)) {

                                    log.info(
                                            "[Realtime STT] Python/OpenAI ready. sessionId={}",
                                            sessionId
                                    );

                                    return;
                                }


                                /*
                                 * =============================
                                 * ERROR
                                 * =============================
                                 */
                                if ("error".equals(type)) {

                                    log.error(
                                            "[Realtime STT] Python/OpenAI error. sessionId={}, payload={}",
                                            sessionId,
                                            payload
                                    );

                                    return;
                                }


                                log.debug(
                                        "[Realtime STT] Unknown message. sessionId={}, payload={}",
                                        sessionId,
                                        payload
                                );


                            } catch (Exception e) {

                                log.error(
                                        "[Realtime STT] Failed to parse Python message. sessionId={}, payload={}",
                                        sessionId,
                                        payload,
                                        e
                                );
                            }
                        }


                        /*
                         * Python 연결 오류
                         */
                        @Override
                        public void handleTransportError(
                                WebSocketSession session,
                                Throwable exception
                        ) {

                            log.error(
                                    "[Realtime STT Client] Python WebSocket error. sessionId={}",
                                    sessionId,
                                    exception
                            );
                        }


                        /*
                         * Python 연결 종료
                         */
                        @Override
                        public void afterConnectionClosed(
                                WebSocketSession session,
                                CloseStatus status
                        ) {

                            sessions.remove(sessionId);
                            partialBuffers.remove(sessionId);

                            log.info(
                                    "[Realtime STT Client] Python disconnected. sessionId={}, status={}",
                                    sessionId,
                                    status
                            );
                        }
                    },

                    realtimeUrl

            ).get(
                    5,
                    TimeUnit.SECONDS
            );


            /*
             * WebSocket은 동시에 여러 sendMessage가 발생하면
             * 문제가 생길 수 있으므로 decorator로 감싼다.
             */
            WebSocketSession safeSession =
                    new ConcurrentWebSocketSessionDecorator(
                            rawSession,

                            // send 최대 대기 시간
                            10_000,

                            // send buffer 최대 1MB
                            1024 * 1024
                    );


            sessions.put(
                    sessionId,
                    safeSession
            );


            log.info(
                    "[Realtime STT Client] Connection stored. sessionId={}",
                    sessionId
            );


        } catch (Exception e) {

            log.error(
                    "[Realtime STT Client] Failed to connect Python. sessionId={}",
                    sessionId,
                    e
            );

            throw new IllegalStateException(
                    "Python Realtime STT 연결 실패",
                    e
            );
        }
    }


    /*
     * ==============================
     * PCM -> Python
     * ==============================
     */
    public void sendAudio(
            Long sessionId,
            byte[] pcmBytes
    ) {

        WebSocketSession session =
                getConnectedSession(sessionId);


        try {

            session.sendMessage(
                    new BinaryMessage(pcmBytes)
            );


            log.debug(
                    "[Realtime STT -> Python] PCM sent. sessionId={}, bytes={}",
                    sessionId,
                    pcmBytes.length
            );


        } catch (IOException e) {

            throw new IllegalStateException(
                    "PCM 전송 실패. sessionId=" + sessionId,
                    e
            );
        }
    }


    /*
     * ==============================
     * commit / clear 등의
     * 제어 메시지 -> Python
     * ==============================
     */
    public void sendControl(
            Long sessionId,
            String message
    ) {

        WebSocketSession session =
                getConnectedSession(sessionId);


        try {

            session.sendMessage(
                    new TextMessage(message)
            );


            log.info(
                    "[Realtime STT -> Python] Control sent. sessionId={}, message={}",
                    sessionId,
                    message
            );


        } catch (IOException e) {

            throw new IllegalStateException(
                    "Realtime STT 제어 메시지 전송 실패. sessionId="
                            + sessionId,
                    e
            );
        }
    }


    /*
     * ==============================
     * 연결 종료
     * ==============================
     */
    public void close(Long sessionId) {

        WebSocketSession session =
                sessions.remove(sessionId);


        if (session == null) {
            return;
        }


        try {

            if (session.isOpen()) {

                session.close(
                        CloseStatus.NORMAL
                );
            }


        } catch (IOException e) {

            log.warn(
                    "[Realtime STT Client] Close failed. sessionId={}",
                    sessionId,
                    e
            );
        }
    }


    /*
     * ==============================
     * 연결 확인
     * ==============================
     */
    private WebSocketSession getConnectedSession(
            Long sessionId
    ) {

        WebSocketSession session =
                sessions.get(sessionId);


        if (
                session == null ||
                        !session.isOpen()
        ) {

            throw new IllegalStateException(
                    "Python Realtime STT가 연결되어 있지 않습니다. sessionId="
                            + sessionId
            );
        }


        return session;
    }
}