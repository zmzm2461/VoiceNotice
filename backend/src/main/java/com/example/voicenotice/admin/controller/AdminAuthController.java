package com.example.voicenotice.admin.controller;

import com.example.voicenotice.admin.service.AdminAuthService;
import com.example.voicenotice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<AdminAuthService.AdminLoginResponse> login(
            @RequestBody AdminLoginRequest request
    ) {
        return ApiResponse.ok(
                adminAuthService.login(request.adminId(), request.password())
        );
    }

    public record AdminLoginRequest(
            String adminId,
            String password
    ) {}
}