package com.example.voicenotice.api.notice.dto;

import java.time.LocalDateTime;

public class NoticeResponse {

    private Long id;
    private String finalText;
    private String summary;
    private String category;
    private LocalDateTime createdAt;

    public NoticeResponse(Long id, String finalText, String summary, String category, LocalDateTime createdAt) {
        this.id = id;
        this.finalText = finalText;
        this.summary = summary;
        this.category = category;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getFinalText() { return finalText; }
    public String getSummary() { return summary; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
