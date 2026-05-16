package com.example.voicenotice.notification.controller;

import com.example.voicenotice.notification.service.PushTokenService;
import com.example.voicenotice.user.entity.User;
import com.example.voicenotice.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final PushTokenService service;
    private final UserService userService;

    @PostMapping
    public void saveToken(
            @RequestBody TokenRequest request,
            HttpServletRequest httpRequest
    ) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User user = userService.getById(userId);

        service.save(request.getToken(), user);
    }

    public static class TokenRequest {

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}