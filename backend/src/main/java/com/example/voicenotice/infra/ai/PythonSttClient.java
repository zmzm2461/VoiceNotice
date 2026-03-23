package com.example.voicenotice.infra.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class PythonSttClient implements SttClient {

    private final WebClient webClient;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.stt-path:/stt}")
    private String sttPath;

    public PythonSttClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public String stt(byte[] audioBytes, String filename) {
        ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename == null ? "audio.wav" : filename;
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);

        // Python 응답이 {"rawText":"..."} 라고 가정
        Map<String, Object> res = webClient.post()
                .uri(baseUrl + sttPath)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Object raw = res == null ? null : res.get("rawText");
        if (raw == null) throw new IllegalStateException("STT failed: rawText missing");
        return raw.toString();
    }
}
