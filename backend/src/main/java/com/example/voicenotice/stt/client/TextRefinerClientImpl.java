package com.example.voicenotice.stt.client;

import com.example.voicenotice.stt.dto.RefineRequest;
import com.example.voicenotice.stt.dto.RefineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TextRefinerClientImpl implements TextRefinerClient {

    private final RestTemplate restTemplate;

    private final String aiServerUrl = "http://localhost:8000";

    @Override
    public String refine(String text) {
        RefineRequest request = new RefineRequest(text);

        RefineResponse response = restTemplate.postForObject(
                aiServerUrl + "/refine",
                request,
                RefineResponse.class
        );

        return response.refinedText();
    }

    @Override
    public String summarize(String text) {
        Map<String, String> request = Map.of("text", text);

        Map response = restTemplate.postForObject(
                aiServerUrl + "/summarize",
                request,
                Map.class
        );

        return (String) response.get("summary");
    }
}