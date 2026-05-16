package com.example.voicenotice.notification.service;

import com.example.voicenotice.notification.entity.PushToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushTokenService tokenService;
    private final WebClient webClient;

    public void sendToAll(String title, String body) {

        List<PushToken> tokens = tokenService.findAll();

        for (PushToken token : tokens) {
            send(token.getToken(), title, body);
        }
    }

    private void send(String token, String title, String body) {
        try {
            URL url = new URL("https://exp.host/--/api/v2/push/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = """
            {
              "to": "%s",
              "title": "%s",
              "body": "%s"
            }
            """.formatted(token, title, body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            conn.getResponseCode();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToToken(String token, String title, String body) {
        try {
            Map<String, Object> message = Map.of(
                    "to", token,
                    "title", title,
                    "body", body
            );

            webClient.post()
                    .uri("https://exp.host/--/api/v2/push/send")
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (Exception e) {
            System.out.println("푸시 발송 실패: " + e.getMessage());
        }
    }

}