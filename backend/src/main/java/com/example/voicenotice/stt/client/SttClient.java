package com.example.voicenotice.stt.client;

import java.math.BigDecimal;

public interface SttClient {
    SttResult stt(byte[] audioBytes, String filename);

    record SttResult(String text, BigDecimal confidence) {}
}
