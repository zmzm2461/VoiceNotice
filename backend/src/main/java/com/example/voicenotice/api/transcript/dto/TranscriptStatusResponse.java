package com.example.voicenotice.api.transcript.dto;

public class TranscriptStatusResponse {

    private Long transcriptId;
    private String status;
    private String errorMessage;

    public TranscriptStatusResponse(Long transcriptId, String status, String errorMessage) {
        this.transcriptId = transcriptId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Long getTranscriptId() {
        return transcriptId;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}