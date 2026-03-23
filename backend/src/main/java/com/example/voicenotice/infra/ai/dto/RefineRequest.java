package com.example.voicenotice.infra.ai.dto;

public class RefineRequest {
    private String rawText;
    private String style;
    private Integer maxLength;

    public RefineRequest(String rawText, String style, Integer maxLength) {
        this.rawText = rawText;
        this.style = style;
        this.maxLength = maxLength;
    }

    public String getRawText() { return rawText; }
    public String getStyle() { return style; }
    public Integer getMaxLength() { return maxLength; }
}
