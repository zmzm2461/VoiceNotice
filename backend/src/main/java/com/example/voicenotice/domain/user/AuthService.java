package com.example.voicenotice.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateSocialUser(String email, String name, AuthProvider provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(user -> {
                    user.updateSocialProfile(name, email);
                    return user;
                })
                .orElseGet(() -> userRepository.save(new User(email, name, provider, providerId)));
    }
}