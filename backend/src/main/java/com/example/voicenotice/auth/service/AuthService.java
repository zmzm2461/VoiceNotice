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

    public LoginResponse loginWithKakao(
            String authorizationCode
    ) {
        /*
         * 1. 인가코드를 카카오 Access Token으로 교환
         */
        String kakaoAccessToken =
                kakaoAuthClient.exchangeAuthorizationCode(
                        authorizationCode
                );

        /*
         * 2. 카카오 사용자 정보 조회
         */
        KakaoUserInfo kakaoUserInfo =
                kakaoAuthClient.getUserInfo(
                        kakaoAccessToken
                );

        /*
         * 3. 기존 사용자 조회 또는 신규 사용자 생성
         */
        User user = userService.findOrCreate(
                "KAKAO",
                kakaoUserInfo.providerUserId(),
                kakaoUserInfo.name(),
                kakaoUserInfo.email()
        );

        /*
         * 4. 서비스 JWT 발급
         */
        String jwt =
                jwtTokenProvider.createAccessToken(user);

        return new LoginResponse(
                jwt,
                user.getId(),
                user.getName()
        );
    }

    public record LoginResponse(
            String accessToken,
            Long id,
            String name
    ) {
    }
}