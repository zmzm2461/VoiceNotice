package com.example.voicenotice.auth.client;

import com.example.voicenotice.auth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private final WebClient webClient;

    public KakaoUserInfo getUserInfo(String accessToken) {
        Map<String, Object> response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalArgumentException("카카오 사용자 정보를 가져오지 못했습니다.");
        }

        String id = String.valueOf(response.get("id"));

        Map<String, Object> kakaoAccount =
                (Map<String, Object>) response.get("kakao_account");

        Map<String, Object> profile =
                kakaoAccount == null ? null : (Map<String, Object>) kakaoAccount.get("profile");

        String email = kakaoAccount == null ? null : (String) kakaoAccount.get("email");
        String name = profile == null ? "카카오 사용자" : (String) profile.get("nickname");

        return new KakaoUserInfo(id, name, email);
    }
}