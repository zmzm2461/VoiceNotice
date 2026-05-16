package com.example.voicenotice.stt.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
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
    public SttResult stt(byte[] audioBytes, String filename) {
        ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename == null ? "audio.wav" : filename;
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);

        Map<String, Object> res = webClient.post()
                .uri(baseUrl + sttPath)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (res == null) {
            throw new IllegalStateException("STT failed: empty response");
        }

        Object text = res.get("text");
        if (text == null) text = res.get("rawText");
        if (text == null) {
            throw new IllegalStateException("STT failed: text/rawText missing");
        }

        BigDecimal confidence = null;
        Object confidenceObj = res.get("confidence");
        if (confidenceObj != null) {
            try {
                confidence = new BigDecimal(confidenceObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        return new SttResult(text.toString(), confidence);
    }
}
