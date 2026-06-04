package com.example.voicenotice.transcript.dto;

import com.example.voicenotice.transcript.entity.FinalTranscript;

public record FinalizeResult(
        FinalTranscript finalTranscript,
        Long logId
) {
}