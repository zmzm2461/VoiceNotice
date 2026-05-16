package com.example.voicenotice.stt.client;

public interface TextRefinerClient {
    String refine(String text);

    String summarize(String text);
}
