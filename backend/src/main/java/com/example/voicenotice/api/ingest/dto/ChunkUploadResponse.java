package com.example.voicenotice.api.ingest.dto;

public class ChunkUploadResponse {
    private String uploadId;
    private String status; // RECEIVED / ASSEMBLED / OUT_OF_ORDER / DUPLICATE
    private int nextChunkIndex; // 서버가 기대하는 다음 chunk 번호

    public ChunkUploadResponse(String uploadId, String status, int nextChunkIndex) {
        this.uploadId = uploadId;
        this.status = status;
        this.nextChunkIndex = nextChunkIndex;
    }

    public String getUploadId() { return uploadId; }
    public String getStatus() { return status; }
    public int getNextChunkIndex() { return nextChunkIndex; }
}
