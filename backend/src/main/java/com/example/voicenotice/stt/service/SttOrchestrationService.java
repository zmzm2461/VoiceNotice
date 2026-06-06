package com.example.voicenotice.stt.service;

import com.example.voicenotice.audio.entity.AudioChunk;
import com.example.voicenotice.audio.repository.AudioChunkRepository;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import com.example.voicenotice.intercomlog.entity.IntercomLog;
import com.example.voicenotice.intercomlog.service.IntercomLogService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.stt.client.SttClient;
import com.example.voicenotice.stt.client.TextRefinerClient;
import com.example.voicenotice.stt.dto.TranscriptMessage;
import com.example.voicenotice.transcript.dto.FinalizeResult;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import com.example.voicenotice.transcript.entity.TranscriptChunk;
import com.example.voicenotice.transcript.repository.FinalTranscriptRepository;
import com.example.voicenotice.transcript.repository.TranscriptChunkRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
@RequiredArgsConstructor
public class SttOrchestrationService {

    private final SttClient sttClient;
    private final TextRefinerClient textRefinerClient;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final FinalTranscriptRepository finalTranscriptRepository;
    private final AudioChunkRepository audioChunkRepository;
    private final IntercomLogService intercomLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationMessageService conversationMessageService;

    @Async("sttTaskExecutor")
    @Transactional
    public void transcribeChunkAsync(Long audioChunkId, boolean isLast) {
        try {
            System.out.println("[STT 시작] audioChunkId=" + audioChunkId + ", isLast=" + isLast);

            AudioChunk audioChunk = audioChunkRepository.findById(audioChunkId)
                    .orElseThrow(() -> new IllegalArgumentException("AudioChunk not found: " + audioChunkId));

            System.out.println("[STT 파일] sessionId=" + audioChunk.getSession().getId()
                    + ", chunkOrder=" + audioChunk.getChunkOrder()
                    + ", filePath=" + audioChunk.getFilePath());

            TranscriptChunk transcriptChunk = transcribeChunk(audioChunk);

            System.out.println("[STT 완료] sessionId=" + audioChunk.getSession().getId()
                    + ", chunkOrder=" + audioChunk.getChunkOrder()
                    + ", text=" + transcriptChunk.getRawText());

            if (isLast) {
                System.out.println("[세션 최종 처리 시작] sessionId=" + audioChunk.getSession().getId());
                finalizeSession(audioChunk.getSession());
            }

        } catch (Exception e) {
            System.out.println("[STT 실패] audioChunkId=" + audioChunkId);
            e.printStackTrace();
        }
    }

    @Transactional
    public TranscriptChunk transcribeChunk(AudioChunk audioChunk) {
        try {
            byte[] audioBytes = Files.readAllBytes(Path.of(audioChunk.getFilePath()));
            SttClient.SttResult result = sttClient.stt(audioBytes, audioChunk.getFileName());

            TranscriptChunk transcriptChunk = new TranscriptChunk(
                    audioChunk.getSession(),
                    audioChunk.getChunkOrder(),
                    result.text(),
                    result.confidence()
            );

            TranscriptChunk saved = transcriptChunkRepository.save(transcriptChunk);

            conversationMessageService.saveVisitorSttMessage(
                    audioChunk.getSession(),
                    saved.getRawText()
            );

            System.out.println("WebSocket 전송 성공: " + saved.getRawText());

            messagingTemplate.convertAndSend(
                    "/topic/sessions/" + audioChunk.getSession().getId() + "/transcripts",
                    new TranscriptMessage(
                            audioChunk.getSession().getId(),
                            saved.getChunkOrder(),
                            saved.getRawText(),
                            saved.getConfidence()
                    )
            );

            return saved;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to transcribe chunk: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public String getPartialText(Long sessionId) {
        List<TranscriptChunk> chunks =
                transcriptChunkRepository.findBySession_IdOrderByChunkOrderAsc(sessionId);

        return chunks.stream()
                .map(TranscriptChunk::getRawText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String classify(String text) {
        if (text.contains("화재") || text.contains("대피") || text.contains("긴급")) {
            return "EMERGENCY";
        } else if (text.contains("점검") || text.contains("단수") || text.contains("정전")) {
            return "INSPECTION";
        } else {
            return "NOTICE";
        }
    }

    @Transactional
    public FinalTranscript finalizeSession(IntercomSession session) {
        List<TranscriptChunk> chunks =
                transcriptChunkRepository.findBySession_IdOrderByChunkOrderAsc(session.getId());

        String mergedText = chunks.stream()
                .map(TranscriptChunk::getRawText)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b)
                .trim();

        FinalTranscript finalTranscript = finalTranscriptRepository.findBySession_Id(session.getId())
                .orElseGet(() -> finalTranscriptRepository.save(new FinalTranscript(session, mergedText)));

        try {
            String refinedText = textRefinerClient.refine(mergedText);

            String category = classify(refinedText);
            finalTranscript.updateCategory(category);

            finalTranscript.succeed(refinedText);
        } catch (Exception e) {
            String category = classify(mergedText);
            finalTranscript.updateCategory(category);

            finalTranscript.succeed(mergedText);
        }

        intercomLogService.createIfNotExists(finalTranscript);

        return finalTranscript;
    }

    @Transactional(readOnly = true)
    public List<AudioChunk> getAudioChunks(Long sessionId) {
        return audioChunkRepository.findBySession_IdOrderByChunkOrderAsc(sessionId);
    }

    @Transactional
    public FinalizeResult finalizeSessionWithLog(IntercomSession session) {
        FinalTranscript finalTranscript = finalizeSession(session);

        IntercomLog log = intercomLogService.createIfNotExists(finalTranscript);

        return new FinalizeResult(
                finalTranscript,
                log.getId()
        );
    }
}