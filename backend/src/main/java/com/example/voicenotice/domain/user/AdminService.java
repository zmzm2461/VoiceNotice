package com.example.voicenotice.domain.user;

import com.example.voicenotice.domain.user.exception.InvalidAdminInviteCodeException;
import com.example.voicenotice.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Value("${app.admin.invite-code}")
    private String adminInviteCode;

    @Transactional
    public User registerAdminCode(Long userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!adminInviteCode.equals(inviteCode)) {
            throw new InvalidAdminInviteCodeException();
        }

        user.promoteToAdmin();
        return user;
    }
}