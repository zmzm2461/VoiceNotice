package com.example.voicenotice.api.ingest.dto;

public class TranscriptIngestResponse {

    private Long transcriptId;   // long → Long
    private Long noticeId;       // long → Long
    private String status;       // COMPLETED / FAILED

    public TranscriptIngestResponse(Long transcriptId, Long noticeId, String status) {
        this.transcriptId = transcriptId;
        this.noticeId = noticeId;
        this.status = status;
    }

    public Long getTranscriptId() { return transcriptId; }
    public Long getNoticeId() { return noticeId; }
    public String getStatus() { return status; }
}
