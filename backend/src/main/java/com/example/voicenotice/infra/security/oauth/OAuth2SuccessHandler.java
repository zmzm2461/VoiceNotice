package com.example.voicenotice.infra.security.oauth;

import com.example.voicenotice.domain.user.AuthProvider;
import com.example.voicenotice.domain.user.User;
import com.example.voicenotice.domain.user.UserRepository;
import com.example.voicenotice.infra.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        AuthProvider provider = switch (registrationId.toLowerCase()) {
            case "kakao" -> AuthProvider.KAKAO;
            case "google" -> AuthProvider.GOOGLE;
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };

        String providerId = switch (provider) {
            case KAKAO -> String.valueOf(attributes.get("id"));
            case GOOGLE -> String.valueOf(attributes.get("sub"));
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getRole().name()
        );

        String target = redirectUri + "?token=" +
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}