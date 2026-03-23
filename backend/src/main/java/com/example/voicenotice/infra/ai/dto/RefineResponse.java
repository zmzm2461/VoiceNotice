package com.example.voicenotice.infra.ai.dto;

public class RefineResponse {
    private String finalText;
    private String summary;
    private String category;
    private Double confidence;

    public String getFinalText() { return finalText; }
    public String getSummary() { return summary; }
    public String getCategory() { return category; }
    public Double getConfidence() { return confidence; }

    public void setFinalText(String finalText) { this.finalText = finalText; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setCategory(String category) { this.category = category; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
