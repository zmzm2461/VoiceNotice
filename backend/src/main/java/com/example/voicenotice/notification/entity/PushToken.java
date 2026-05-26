package com.example.voicenotice.notification.entity;

import com.example.voicenotice.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_tokens")
@Getter
@NoArgsConstructor
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PushToken(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public void updateUser(User user) {
        this.user = user;
    }
}