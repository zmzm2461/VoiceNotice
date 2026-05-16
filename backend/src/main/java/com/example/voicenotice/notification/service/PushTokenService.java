package com.example.voicenotice.notification.service;

import com.example.voicenotice.notification.entity.PushToken;
import com.example.voicenotice.notification.repository.PushTokenRepository;
import com.example.voicenotice.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenRepository repository;

    public void save(String token, User user) {

        repository.findByToken(token)
                .orElseGet(() -> repository.save(new PushToken(token, user)));
    }

    public List<PushToken> findAll() {
        return repository.findAll();
    }
}