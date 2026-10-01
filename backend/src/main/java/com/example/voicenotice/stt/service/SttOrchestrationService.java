package com.example.voicenotice.stt.service;

import com.example.voicenotice.audio.entity.AudioChunk;
import com.example.voicenotice.audio.repository.AudioChunkRepository;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import com.example.voicenotice.intercomlog.entity.IntercomLog;
import com.example.voicenotice.intercomlog.service.IntercomLogService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import com.example.voicenotice.transcript.dto.FinalizeResult;
import com.example.voicenotice.transcript.entity.FinalTranscript;
import com.example.voicenotice.transcript.entity.TranscriptChunk;
import com.example.voicenotice.transcript.repository.FinalTranscriptRepository;
import com.example.voicenotice.transcript.repository.TranscriptChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SttOrchestrationService {

    private final TranscriptChunkRepository transcriptChunkRepository;
    private final FinalTranscriptRepository finalTranscriptRepository;
    private final AudioChunkRepository audioChunkRepository;
    private final IntercomLogService intercomLogService;
    private final ConversationMessageService conversationMessageService;
    private final IntercomSessionRepository intercomSessionRepository;


    /**
     * 현재까지 저장된 Realtime STT transcript 조회
     */
    @Transactional(readOnly = true)
    public String getPartialText(Long sessionId) {

        List<TranscriptChunk> chunks =
                transcriptChunkRepository
                        .findBySession_IdOrderByChunkOrderAsc(
                                sessionId
                        );

        return chunks.stream()
                .map(TranscriptChunk::getRawText)
                .filter(
                        text ->
                                text != null
                                        && !text.isBlank()
                )
                .collect(
                        Collectors.joining(" ")
                );
    }


    /**
     * 최종 transcript 카테고리 분류
     */
    private String classify(String text) {

        if (text == null || text.isBlank()) {
            return "NOTICE";
        }

        if (
                text.contains("화재")
                        || text.contains("대피")
                        || text.contains("긴급")
                        || text.contains("응급")
                        || text.contains("위험")
        ) {

            return "EMERGENCY";

        } else if (
                text.contains("점검")
                        || text.contains("단수")
                        || text.contains("정전")
                        || text.contains("관리사무소")
                        || text.contains("경비실")
        ) {

            return "INSPECTION";

        } else if (
                text.contains("택배")
                        || text.contains("배달")
                        || text.contains("우편")
        ) {

            return "DELIVERY";

        } else {

            return "NOTICE";
        }
    }


    /**
     * 세션 종료 시
     * 저장된 Realtime STT 문장들을 하나로 합쳐
     * FinalTranscript 생성
     */
    @Transactional
    public FinalTranscript finalizeSession(
            IntercomSession session
    ) {

        List<TranscriptChunk> chunks =
                transcriptChunkRepository
                        .findBySession_IdOrderByChunkOrderAsc(
                                session.getId()
                        );


        String mergedText =
                chunks.stream()
                        .map(
                                TranscriptChunk::getRawText
                        )
                        .filter(
                                text ->
                                        text != null
                                                && !text.isBlank()
                        )
                        .collect(
                                Collectors.joining(" ")
                        )
                        .trim();


        FinalTranscript finalTranscript =
                finalTranscriptRepository
                        .findBySession_Id(
                                session.getId()
                        )
                        .orElseGet(
                                () ->
                                        finalTranscriptRepository.save(
                                                new FinalTranscript(
                                                        session,
                                                        mergedText
                                                )
                                        )
                        );


        String category =
                classify(mergedText);


        finalTranscript.updateCategory(
                category
        );


        finalTranscript.succeed(
                mergedText
        );


        System.out.println(
                "[세션 종료 FinalTranscript 생성]"
                        + " sessionId="
                        + session.getId()
                        + ", mergedText="
                        + mergedText
                        + ", category="
                        + category
        );


        return finalTranscript;
    }


    /**
     * 기존 저장된 AudioChunk 조회용
     *
     * 관리자 화면/기존 기능 때문에 일단 유지
     */
    @Transactional(readOnly = true)
    public List<AudioChunk> getAudioChunks(
            Long sessionId
    ) {

        return audioChunkRepository
                .findBySession_IdOrderByChunkOrderAsc(
                        sessionId
                );
    }


    /**
     * 세션 종료 로그 생성
     */
    @Transactional
    public FinalizeResult finalizeSessionWithLog(
            IntercomSession session
    ) {

        FinalTranscript finalTranscript =
                finalizeSession(
                        session
                );


        IntercomLog log =
                intercomLogService
                        .createIfNotExists(
                                finalTranscript
                        );


        return new FinalizeResult(
                finalTranscript,
                log.getId()
        );
    }


    /**
     * OpenAI Realtime STT에서
     * final 문장이 왔을 때 호출
     */
    @Transactional
    public TranscriptChunk saveRealtimeFinal(
            Long sessionId,
            String finalText
    ) {

        if (
                finalText == null
                        || finalText.isBlank()
        ) {

            System.out.println(
                    "[Realtime FINAL 저장 생략]"
                            + " 빈 텍스트. sessionId="
                            + sessionId
            );

            return null;
        }


        /*
         * 1. 인터폰 세션 조회
         */
        IntercomSession intercomSession =
                intercomSessionRepository
                        .findById(
                                sessionId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "IntercomSession not found: "
                                                        + sessionId
                                        )
                        );


        /*
         * 2. 다음 chunkOrder 계산
         */
        int nextChunkOrder =
                transcriptChunkRepository
                        .findTopBySession_IdOrderByChunkOrderDesc(
                                sessionId
                        )
                        .map(
                                chunk ->
                                        chunk.getChunkOrder()
                                                + 1
                        )
                        .orElse(0);


        /*
         * 3. Realtime STT 결과 생성
         *
         * 현재 Realtime STT에서는
         * confidence를 따로 사용하지 않으므로 null
         */
        TranscriptChunk transcriptChunk =
                new TranscriptChunk(
                        intercomSession,
                        nextChunkOrder,
                        finalText.trim(),
                        null
                );


        /*
         * 4. transcript_chunks 저장
         */
        TranscriptChunk saved =
                transcriptChunkRepository.save(
                        transcriptChunk
                );


        /*
         * 5. 방문자 채팅 메시지 저장
         *
         * 동시에
         * /topic/sessions/{sessionId}/messages
         * 로 프론트에도 전달
         */
        conversationMessageService
                .saveVisitorSttMessage(
                        intercomSession,
                        finalText.trim()
                );


        System.out.println(
                "[Realtime FINAL DB 저장 완료]"
                        + " sessionId="
                        + sessionId
                        + ", chunkOrder="
                        + nextChunkOrder
                        + ", text="
                        + saved.getRawText()
        );


        return saved;
    }
}