package com.example.voicenotice.tts.client;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;


@Component
@RequiredArgsConstructor
public class TtsClient {


    private final WebClient webClient;


    @Value("${app.ai.base-url}")
    private String aiUrl;

    @Value("${app.ai.tts-path:/tts}")
    private String ttsPath;


    public Flux<DataBuffer> requestTts(String text) {

        return webClient.post()
                .uri(aiUrl + ttsPath)
                .bodyValue(
                        Map.of("text", text)
                )
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }

}