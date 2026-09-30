package com.example.voicenotice.tts.controller;


import com.example.voicenotice.tts.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
public class TtsController {


    private final TtsService ttsService;


    @PostMapping(
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public Flux<DataBuffer> tts(
            @RequestBody TtsRequest request
    ){

        return ttsService.generate(
                request.text()
        );

    }



    public record TtsRequest(
            String text
    ){}

}