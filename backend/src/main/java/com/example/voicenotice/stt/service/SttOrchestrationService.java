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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
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

    /**
     * 현재 구조:
     * 하드웨어가 방문자의 발화가 끝난 뒤 완성된 음성파일 1개를 전송한다.
     *
     * 따라서 더 이상 session 전체 transcript를 누적해서 refine하지 않고,
     * 이번 audioChunk에서 나온 STT 결과만 GPT 후처리 후 채팅 메시지로 저장한다.
     */
    @Async("sttTaskExecutor")
    @Transactional
    public void transcribeChunkAsync(Long audioChunkId, boolean isLast) {
        try {
            System.out.println("[STT 시작] audioChunkId=" + audioChunkId + ", isLast=" + isLast);

            AudioChunk audioChunk = audioChunkRepository.findById(audioChunkId)
                    .orElseThrow(() -> new IllegalArgumentException("AudioChunk not found: " + audioChunkId));

            TranscriptChunk transcriptChunk = transcribeChunk(audioChunk);

            if (transcriptChunk == null ||
                    transcriptChunk.getRawText() == null ||
                    transcriptChunk.getRawText().isBlank()) {

                System.out.println("[STT 빈 결과 스킵] sessionId="
                        + audioChunk.getSession().getId()
                        + ", chunkOrder=" + audioChunk.getChunkOrder());

                return;
            }

            String rawText = transcriptChunk.getRawText();

            System.out.println("[STT 완료] sessionId=" + audioChunk.getSession().getId()
                    + ", chunkOrder=" + audioChunk.getChunkOrder()
                    + ", rawText=" + rawText);

            String refinedText;

            try {
                System.out.println("[현재 음성파일 AI 후처리 시작] sessionId="
                        + audioChunk.getSession().getId()
                        + ", chunkOrder=" + audioChunk.getChunkOrder());

                refinedText = textRefinerClient.refine(rawText);

                if (refinedText == null || refinedText.isBlank()) {
                    refinedText = rawText;
                }

                System.out.println("[현재 음성파일 AI 후처리 완료] refinedText=" + refinedText);

            } catch (Exception e) {
                System.out.println("[AI 후처리 실패 - 원문 저장] " + e.getMessage());
                refinedText = rawText;
            }

            conversationMessageService.saveVisitorSttMessage(
                    audioChunk.getSession(),
                    refinedText
            );

            System.out.println("[채팅 저장 완료] sessionId="
                    + audioChunk.getSession().getId()
                    + ", chunkOrder=" + audioChunk.getChunkOrder()
                    + ", savedText=" + refinedText);

            /**
             * 주의:
             * 기존에는 isLast=true일 때 finalizeSession()을 호출했지만,
             * 현재 구조에서는 매 음성파일이 이미 하나의 완성 발화이므로
             * 여기서 finalizeSession()을 호출하지 않는다.
             *
             * 세션 종료 로그가 필요하면 SessionService.endSession() 등에서
             * finalizeSessionWithLog()를 한 번만 호출하는 방식으로 분리한다.
             */

        } catch (Exception e) {
            System.out.println("[STT 실패] audioChunkId=" + audioChunkId);
            e.printStackTrace();
        }
    }

    /**
     * STT 변환 + transcript_chunks 저장 + WebSocket 실시간 전송만 담당.
     * 채팅 메시지 저장은 transcribeChunkAsync()에서 GPT 후처리 후 수행한다.
     */
    @Transactional
    public TranscriptChunk transcribeChunk(AudioChunk audioChunk) {
        try {
            byte[] audioBytes = Files.readAllBytes(Path.of(audioChunk.getFilePath()));

            SttClient.SttResult result = sttClient.stt(
                    audioBytes,
                    audioChunk.getFileName()
            );

            if (result.text() == null || result.text().isBlank()) {
                System.out.println("[STT 결과 없음 - 저장/전송 생략] sessionId="
                        + audioChunk.getSession().getId()
                        + ", chunkOrder=" + audioChunk.getChunkOrder());

                return null;
            }

            TranscriptChunk transcriptChunk = new TranscriptChunk(
                    audioChunk.getSession(),
                    audioChunk.getChunkOrder(),
                    result.text(),
                    result.confidence()
            );

            TranscriptChunk saved = transcriptChunkRepository.save(transcriptChunk);

            System.out.println("[WebSocket 전송] sessionId="
                    + audioChunk.getSession().getId()
                    + ", chunkOrder=" + saved.getChunkOrder()
                    + ", rawText=" + saved.getRawText());

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

    /**
     * 현재까지 저장된 transcript_chunks 원문 조회.
     * 관리자 화면 또는 디버깅용으로 유지.
     */
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
        if (text == null || text.isBlank()) {
            return "NOTICE";
        }

        if (text.contains("화재") || text.contains("대피") || text.contains("긴급")
                || text.contains("응급") || text.contains("위험")) {
            return "EMERGENCY";
        } else if (text.contains("점검") || text.contains("단수") || text.contains("정전")
                || text.contains("관리사무소") || text.contains("경비실")) {
            return "INSPECTION";
        } else if (text.contains("택배") || text.contains("배달") || text.contains("우편")) {
            return "DELIVERY";
        } else {
            return "NOTICE";
        }
    }

    /**
     * 세션 종료 시 최종 로그 생성용.
     *
     * 주의:
     * 여기서는 GPT 후처리를 다시 하지 않는다.
     * 이미 각 음성파일마다 GPT 후처리된 메시지가 conversation_messages에 저장되기 때문이다.
     *
     * final_transcripts에는 transcript_chunks의 원문을 병합해서 보관한다.
     */
    @Transactional
    public FinalTranscript finalizeSession(IntercomSession session) {
        List<TranscriptChunk> chunks =
                transcriptChunkRepository.findBySession_IdOrderByChunkOrderAsc(session.getId());

        String mergedText = chunks.stream()
                .map(TranscriptChunk::getRawText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining(" "))
                .trim();

        FinalTranscript finalTranscript = finalTranscriptRepository.findBySession_Id(session.getId())
                .orElseGet(() -> finalTranscriptRepository.save(new FinalTranscript(session, mergedText)));

        String category = classify(mergedText);

        finalTranscript.updateCategory(category);
        finalTranscript.succeed(mergedText);

        intercomLogService.createIfNotExists(finalTranscript);

        System.out.println("[세션 종료 로그 생성] sessionId="
                + session.getId()
                + ", mergedText=" + mergedText
                + ", category=" + category);

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