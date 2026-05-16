package com.example.voicenotice.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
        }
)
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 150)
    private String providerUserId;

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User(String provider, String providerUserId, String name, String email) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.name = name;
        this.email = email;
        this.role = "RESIDENT";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }
}