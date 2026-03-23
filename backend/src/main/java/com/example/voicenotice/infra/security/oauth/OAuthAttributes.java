package com.example.voicenotice.infra.security.oauth;

import com.example.voicenotice.domain.user.AuthProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {

    private AuthProvider provider;
    private String providerId;
    private String email;
    private String name;
    private Map<String, Object> attributes;
    private String nameAttributeKey;

    public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        if ("google".equals(registrationId)) {
            return ofGoogle(userNameAttributeName, attributes);
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .provider(AuthProvider.GOOGLE)
                .providerId(String.valueOf(attributes.get("sub")))
                .email((String) attributes.get("email"))
                .name((String) attributes.getOrDefault("name", "사용자"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties =
                (Map<String, Object>) attributes.get("properties");

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = properties != null ? (String) properties.get("nickname") : "사용자";

        return OAuthAttributes.builder()
                .provider(AuthProvider.KAKAO)
                .providerId(String.valueOf(attributes.get("id")))
                .email(email)
                .name(nickname)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}