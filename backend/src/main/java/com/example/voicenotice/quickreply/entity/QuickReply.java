package com.example.voicenotice.quickreply.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class QuickReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer replyCode;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Boolean active = true;

    public QuickReply(Integer replyCode, String text) {
        this.replyCode = replyCode;
        this.text = text;
        this.active = true;
    }
}