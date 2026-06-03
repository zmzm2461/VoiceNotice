package com.example.voicenotice.admin.service;

import com.example.voicenotice.auth.jwt.JwtTokenProvider;
import com.example.voicenotice.user.entity.User;
import com.example.voicenotice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminLoginResponse login(String adminId, String password) {

        User user = userRepository.findByProviderUserId(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

        if (!"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("관리자 권한이 없습니다.");
        }

        // 임시 비밀번호 방식
        // 나중에는 DB에 password 컬럼 만들고 암호화해서 비교해야 함
        if (!"admin1234".equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createAccessToken(user);

        return new AdminLoginResponse(
                token,
                user.getName(),
                user.getRole()
        );
    }

    public record AdminLoginResponse(
            String accessToken,
            String name,
            String role
    ) {}
}