package com.example.voicenotice.api.ingest.dto;

public class ChunkStatusResponse {
    private String uploadId;
    private String status; // UPLOADING / ASSEMBLED / NOT_FOUND
    private int nextChunkIndex;
    private Integer totalChunks;

    public ChunkStatusResponse(String uploadId, String status, int nextChunkIndex, Integer totalChunks) {
        this.uploadId = uploadId;
        this.status = status;
        this.nextChunkIndex = nextChunkIndex;
        this.totalChunks = totalChunks;
    }

    public String getUploadId() { return uploadId; }
    public String getStatus() { return status; }
    public int getNextChunkIndex() { return nextChunkIndex; }
    public Integer getTotalChunks() { return totalChunks; }
}
