package com.example.voicenotice.auth.service;

import com.example.voicenotice.auth.client.KakaoAuthClient;
import com.example.voicenotice.auth.dto.KakaoUserInfo;
import com.example.voicenotice.auth.jwt.JwtTokenProvider;
import com.example.voicenotice.user.entity.User;
import com.example.voicenotice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse loginWithKakao(String accessToken) {
        KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(accessToken);

        User user = userService.findOrCreate(
                "KAKAO",
                kakaoUserInfo.providerUserId(),
                kakaoUserInfo.name(),
                kakaoUserInfo.email()
        );

        String jwt = jwtTokenProvider.createAccessToken(user);

        return new LoginResponse(
                jwt,
                user.getName()
        );
    }

    public record LoginResponse(
            String accessToken,
            String name
    ) {}
}