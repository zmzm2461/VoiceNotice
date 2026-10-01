package com.example.voicenotice.tts.service;

import com.example.voicenotice.audio.websocket.TtsAudioWebSocketHandler;
import com.example.voicenotice.tts.client.TtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class TtsService {

    private final TtsClient ttsClient;
    private final TtsAudioWebSocketHandler ttsAudioWebSocketHandler;


    public Mono<Void> sendTts(
            String deviceId,
            String text
    ) {

        if (text == null || text.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException(
                            "TTS text는 비어 있을 수 없습니다."
                    )
            );
        }

        String trimmedText = text.trim();

        System.out.println(
                "[TTS] 생성 시작"
                        + " deviceId=" + deviceId
                        + ", text=" + trimmedText
        );


        /*
         * 1. ESP32에게 TTS 재생 시작 알림
         *
         * ESP32는 이 메시지를 받으면
         * 마이크 → STT 전송을 잠시 중단한다.
         */
        return Mono.fromRunnable(
                        () -> ttsAudioWebSocketHandler.sendStart(
                                deviceId
                        )
                )

                /*
                 * 2. Python /tts 호출
                 *
                 * OpenAI TTS에서 만들어지는 PCM을
                 * Flux<DataBuffer> 형태로 계속 받는다.
                 */
                .thenMany(
                        ttsClient
                                .requestTts(trimmedText)

                                /*
                                 * 3. PCM 조각이 들어올 때마다
                                 * ESP32 WebSocket으로 즉시 전달
                                 */
                                .doOnNext(buffer -> {

                                    byte[] pcm =
                                            new byte[
                                                    buffer.readableByteCount()
                                                    ];

                                    try {

                                        buffer.read(pcm);

                                        ttsAudioWebSocketHandler
                                                .sendAudio(
                                                        deviceId,
                                                        pcm
                                                );

                                    } finally {

                                        /*
                                         * DataBuffer 메모리 반환
                                         */
                                        DataBufferUtils.release(
                                                buffer
                                        );
                                    }
                                })
                )

                /*
                 * Flux 종료를 Mono<Void>로 변환
                 */
                .then()

                /*
                 * 성공/실패/취소 여부와 관계없이
                 * 반드시 tts_end 전달
                 *
                 * 그래야 ESP32 마이크가 다시 활성화됨
                 */
                .doFinally(signalType -> {

                    ttsAudioWebSocketHandler.sendEnd(
                            deviceId
                    );

                    System.out.println(
                            "[TTS] 전송 종료"
                                    + " deviceId=" + deviceId
                                    + ", signal=" + signalType
                    );
                });
    }
}