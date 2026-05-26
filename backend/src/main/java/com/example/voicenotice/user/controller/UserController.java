package com.example.voicenotice.user.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.user.dto.UserResponse;
import com.example.voicenotice.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return ApiResponse.ok(userService.getMe(userId));
    }
}