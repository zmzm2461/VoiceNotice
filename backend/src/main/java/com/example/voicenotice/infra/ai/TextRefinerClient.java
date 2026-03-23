package com.example.voicenotice.infra.ai;

import com.example.voicenotice.infra.ai.dto.RefineResponse;

public interface TextRefinerClient {
    RefineResponse refine(String rawText);
}
