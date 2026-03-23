package com.example.voicenotice.infra.security.oauth;

import com.example.voicenotice.domain.user.AuthService;
import com.example.voicenotice.domain.user.User;
import com.example.voicenotice.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes oauth = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oauth2User.getAttributes()
        );


        User user = authService.getOrCreateSocialUser(
                oauth.getEmail(),
                oauth.getName(),
                oauth.getProvider(),
                oauth.getProviderId()
        );

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_" + user.getRole().name()),
                oauth.getAttributes(),
                oauth.getNameAttributeKey()
        );
    }
}