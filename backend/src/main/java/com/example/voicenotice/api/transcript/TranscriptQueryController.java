package com.example.voicenotice.api.transcript;

import com.example.voicenotice.api.transcript.dto.TranscriptStatusResponse;
import com.example.voicenotice.domain.transcript.Transcript;
import com.example.voicenotice.domain.transcript.TranscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transcripts")
public class TranscriptQueryController {

    private final TranscriptService transcriptService;

    public TranscriptQueryController(TranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscriptStatusResponse> get(@PathVariable Long id) {

        Transcript t = transcriptService.getOrThrow(id);

        TranscriptStatusResponse response =
                new TranscriptStatusResponse(
                        t.getId(),
                        t.getStatus().name(),
                        t.getErrorMessage()
                );

        return ResponseEntity.ok(response);
    }
}