package com.example.voicenotice.auth.controller;

import com.example.voicenotice.auth.service.AuthService;
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
        return ApiResponse.ok(authService.loginWithKakao(request.accessToken()));
    }

    public record KakaoLoginRequest(
            String accessToken
    ) {}
}