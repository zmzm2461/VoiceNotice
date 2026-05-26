package com.example.voicenotice.user.service;

import com.example.voicenotice.user.dto.UserResponse;
import com.example.voicenotice.user.entity.User;
import com.example.voicenotice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(String provider, String providerUserId, String name, String email) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(user -> {
                    user.updateProfile(name, email);
                    return user;
                })
                .orElseGet(() -> userRepository.save(
                        new User(provider, providerUserId, name, email)
                ));
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getById(userId);
        return UserResponse.from(user);
    }
}