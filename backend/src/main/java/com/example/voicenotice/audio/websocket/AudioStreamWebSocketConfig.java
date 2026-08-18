package com.example.voicenotice.audio.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AudioStreamWebSocketConfig implements WebSocketConfigurer {

    private final AudioStreamWebSocketHandler audioStreamWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {

        registry
                .addHandler(
                        audioStreamWebSocketHandler,
                        "/realtime/audio"
                )
                .setAllowedOriginPatterns("*");
    }
}