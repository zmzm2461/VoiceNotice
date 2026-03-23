package com.example.voicenotice.api.ingest.dto;

import java.time.LocalDateTime;

public class TranscriptIngestRequest {
    private String deviceUid;
    private String rawText;
    private LocalDateTime recordedAt;

    public String getDeviceUid() { return deviceUid; }
    public void setDeviceUid(String deviceUid) { this.deviceUid = deviceUid; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
