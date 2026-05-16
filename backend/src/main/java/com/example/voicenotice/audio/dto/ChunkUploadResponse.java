package com.example.voicenotice.audio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChunkUploadResponse {
    private Long sessionId;
    private Integer chunkOrder;
    private String rawText;
    private String partialText;
    private boolean isLast;
}