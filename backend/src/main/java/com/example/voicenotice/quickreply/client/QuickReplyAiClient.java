package com.example.voicenotice.quickreply.client;

import com.example.voicenotice.quickreply.dto.QuickReplySuggestRequest;
import com.example.voicenotice.quickreply.dto.QuickReplySuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class QuickReplyAiClient {

    private final RestClient restClient;

    public QuickReplyAiClient(
            @Value("${app.ai.base-url}") String aiBaseUrl
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(aiBaseUrl)
                .build();
    }

    public List<QuickReplySuggestionResponse> suggest(
            QuickReplySuggestRequest request
    ) {

        return restClient.post()
                .uri("/quick-replies/suggest")
                .body(request)
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<QuickReplySuggestionResponse>
                                >() {}
                );
    }
}