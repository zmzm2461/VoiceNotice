package com.example.voicenotice.auth.controller;

import com.example.voicenotice.auth.service.AuthService;
import com.example.voicenotice.common.exception.BadRequestException;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao")
    public ApiResponse<AuthService.LoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request
    ) {
        if (
                request == null ||
                        request.authorizationCode() == null ||
                        request.authorizationCode().isBlank()
        ) {
            throw new BadRequestException(
                    "authorizationCode는 필수입니다."
            );
        }

        return ApiResponse.ok(
                authService.loginWithKakao(
                        request.authorizationCode()
                )
        );
    }

    public record KakaoLoginRequest(
            String authorizationCode
    ) {
    }
}