package com.example.voicenotice.api.user;

import com.example.voicenotice.api.user.dto.MeResponse;
import com.example.voicenotice.domain.user.User;
import com.example.voicenotice.domain.user.UserService;
import com.example.voicenotice.global.response.ApiResponse;
import com.example.voicenotice.infra.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getUserById(principal.getUserId());

        MeResponse data = new MeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        return ApiResponse.success(200, "내 정보 조회 성공", data);
    }
}