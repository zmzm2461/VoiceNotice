package com.example.voicenotice.tts.service;


import com.example.voicenotice.tts.client.TtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@RequiredArgsConstructor
public class TtsService {


    private final TtsClient ttsClient;


    public Flux<DataBuffer> generate(String text){

        return ttsClient.requestTts(text);

    }

}