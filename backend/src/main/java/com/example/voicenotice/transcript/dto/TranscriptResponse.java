package com.example.voicenotice.transcript.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TranscriptResponse {

    private String partialText;
    private String finalText;
    private String status;
}