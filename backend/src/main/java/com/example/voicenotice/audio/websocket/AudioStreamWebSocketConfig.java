package com.example.voicenotice.audio.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AudioStreamWebSocketConfig implements WebSocketConfigurer {

    private final AudioStreamWebSocketHandler audioStreamWebSocketHandler;

    // 추가
    private final TtsAudioWebSocketHandler ttsAudioWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {

        // ESP32 마이크 → Spring
        registry
                .addHandler(
                        audioStreamWebSocketHandler,
                        "/realtime/audio"
                )
                .setAllowedOriginPatterns("*");

        // Spring → ESP32 스피커
        registry
                .addHandler(
                        ttsAudioWebSocketHandler,
                        "/tts/audio"
                )
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {

        ServletServerContainerFactoryBean container =
                new ServletServerContainerFactoryBean();

        container.setMaxBinaryMessageBufferSize(64 * 1024);
        container.setMaxTextMessageBufferSize(16 * 1024);

        return container;
    }
}