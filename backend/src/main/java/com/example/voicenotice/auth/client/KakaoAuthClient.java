package com.example.voicenotice.auth.client;

import com.example.voicenotice.auth.dto.KakaoUserInfo;
import com.example.voicenotice.common.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class KakaoAuthClient {

    private static final String KAKAO_TOKEN_URL =
            "https://kauth.kakao.com/oauth/token";

    private static final String KAKAO_USER_INFO_URL =
            "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoAuthClient(
            WebClient webClient,
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.client-secret}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri
    ) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 카카오 인가코드를 카카오 Access Token으로 교환
     */
    public String exchangeAuthorizationCode(String authorizationCode) {

        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("카카오 인가코드가 없습니다.");
        }

        KakaoTokenResponse response = webClient.post()
                .uri(KAKAO_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters
                                .fromFormData(
                                        "grant_type",
                                        "authorization_code"
                                )
                                .with(
                                        "client_id",
                                        clientId
                                )
                                .with(
                                        "redirect_uri",
                                        redirectUri
                                )
                                .with(
                                        "code",
                                        authorizationCode
                                )
                                .with(
                                        "client_secret",
                                        clientSecret
                                )
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse ->
                                clientResponse
                                        .bodyToMono(String.class)
                                        .map(body ->
                                                new BadRequestException(
                                                        "카카오 토큰 발급에 실패했습니다."
                                                )
                                        )
                )
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        if (
                response == null ||
                        response.accessToken() == null ||
                        response.accessToken().isBlank()
        ) {
            throw new BadRequestException(
                    "카카오 Access Token을 받지 못했습니다."
            );
        }

        return response.accessToken();
    }

    /**
     * 카카오 Access Token으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {

        Map<String, Object> response = webClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse ->
                                clientResponse
                                        .bodyToMono(String.class)
                                        .map(body ->
                                                new BadRequestException(
                                                        "카카오 사용자 정보 조회에 실패했습니다."
                                                )
                                        )
                )
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new BadRequestException(
                    "카카오 사용자 정보를 가져오지 못했습니다."
            );
        }

        String id = String.valueOf(response.get("id"));

        Map<String, Object> kakaoAccount =
                (Map<String, Object>) response.get(
                        "kakao_account"
                );

        Map<String, Object> profile =
                kakaoAccount == null
                        ? null
                        : (Map<String, Object>)
                        kakaoAccount.get("profile");

        String email =
                kakaoAccount == null
                        ? null
                        : (String) kakaoAccount.get("email");

        String name =
                profile == null
                        ? "카카오 사용자"
                        : (String) profile.get("nickname");

        return new KakaoUserInfo(
                id,
                name,
                email
        );
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("expires_in")
            Long expiresIn
    ) {
    }
}