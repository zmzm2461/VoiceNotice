package com.example.voicenotice.infra.ai;

public interface SttClient {
    String stt(byte[] audioBytes, String filename);
}
