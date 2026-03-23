package com.example.voicenotice.api.admin;

import com.example.voicenotice.api.admin.dto.AdminCheckResponse;
import com.example.voicenotice.api.admin.dto.AdminCodeRequest;
import com.example.voicenotice.api.admin.dto.AdminCodeResponse;
import com.example.voicenotice.domain.user.AdminService;
import com.example.voicenotice.domain.user.User;
import com.example.voicenotice.global.response.ApiResponse;
import com.example.voicenotice.infra.security.UserPrincipal;
import com.example.voicenotice.infra.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register-code")
    public ApiResponse<AdminCodeResponse> registerAdminCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AdminCodeRequest request
    ) {
        User user = adminService.registerAdminCode(
                principal.getUserId(),
                request.getInviteCode()
        );

        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getRole().name()
        );

        AdminCodeResponse data = new AdminCodeResponse(newAccessToken);

        return ApiResponse.success(200, "관리자 권한 등록 완료", data);
    }

    @GetMapping("/check")
    public ApiResponse<AdminCheckResponse> checkAdmin() {
        AdminCheckResponse data = new AdminCheckResponse("관리자 접근 성공");
        return ApiResponse.success(200, "관리자 권한 확인 성공", data);
    }
}