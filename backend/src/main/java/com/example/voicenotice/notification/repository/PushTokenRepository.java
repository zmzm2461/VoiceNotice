package com.example.voicenotice.notification.repository;

import com.example.voicenotice.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUser_Id(Long userId);

}