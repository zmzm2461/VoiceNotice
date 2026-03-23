package com.example.voicenotice.infra.ai;

import com.example.voicenotice.infra.ai.dto.RefineRequest;
import com.example.voicenotice.infra.ai.dto.RefineResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PythonTextRefinerClient implements TextRefinerClient {

    private final WebClient webClient;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.refine-path:/refine}")
    private String refinePath;

    public PythonTextRefinerClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public RefineResponse refine(String rawText) {
        RefineRequest req = new RefineRequest(rawText, "apartment_notice", 300);

        return webClient.post()
                .uri(baseUrl + refinePath)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(RefineResponse.class)
                .block();
    }
}
