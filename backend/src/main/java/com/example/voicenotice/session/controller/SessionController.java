package com.example.voicenotice.session.controller;

import com.example.voicenotice.common.response.ApiResponse;
import com.example.voicenotice.conversation.dto.ConversationMessageResponse;
import com.example.voicenotice.conversation.service.ConversationMessageService;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.service.SessionService;
import com.example.voicenotice.session.dto.SessionReplyRequest;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final ConversationMessageService conversationMessageService;

    public SessionController(
            SessionService sessionService,
            ConversationMessageService conversationMessageService
    ) {
        this.sessionService = sessionService;
        this.conversationMessageService = conversationMessageService;
    }

    public record StartSessionRequest(String deviceUid) {}
    public record StartSessionResponse(Long sessionId, String status, LocalDateTime startedAt) {}
    public record EndSessionRequest(Long sessionId) {}
    public record EndSessionResponse(Long sessionId, String status) {}
    public record SessionSummaryResponse(Long sessionId, String status, LocalDateTime startedAt, LocalDateTime endedAt) {}
    public record CurrentSessionResponse(Long sessionId, String status, LocalDateTime startedAt) {}

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<StartSessionResponse>> start(@RequestBody StartSessionRequest request) {
        IntercomSession session = sessionService.start(request.deviceUid());
        return ResponseEntity.ok(ApiResponse.ok(
                new StartSessionResponse(
                        session.getId(),
                        session.getStatus().name(),
                        session.getStartedAt()
                )
        ));
    }

    @PostMapping("/end")
    public ResponseEntity<ApiResponse<EndSessionResponse>> end(@RequestBody EndSessionRequest request) {
        IntercomSession session = sessionService.close(request.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(
                new EndSessionResponse(
                        session.getId(),
                        session.getStatus().name()
                )
        ));
    }

    @PostMapping("/{sessionId}/connect")
    public ResponseEntity<Void> connect(@PathVariable Long sessionId) {
        sessionService.connect(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> getByDevice(@RequestParam String deviceUid) {
        List<SessionSummaryResponse> response = sessionService.getByDeviceUid(deviceUid).stream()
                .map(session -> new SessionSummaryResponse(
                        session.getId(),
                        session.getStatus().name(),
                        session.getStartedAt(),
                        session.getEndedAt()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentSessionResponse>> current(@RequestParam String deviceUid) {
        return sessionService.getCurrentByDeviceUid(deviceUid)
                .map(session -> ResponseEntity.ok(ApiResponse.ok(
                        new CurrentSessionResponse(
                                session.getId(),
                                session.getStatus().name(),
                                session.getStartedAt()
                        )
                )))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PostMapping("/{sessionId}/reply")
    public ResponseEntity<ApiResponse<Void>> sendReply(
            @PathVariable Long sessionId,
            @RequestBody SessionReplyRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        sessionService.sendReply(
                userId,
                sessionId,
                request.replyCode()
        );

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ConversationMessageResponse>>> getMessages(
            @PathVariable Long sessionId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        sessionService.validateUserSessionAccess(userId, sessionId);

        List<ConversationMessageResponse> response =
                conversationMessageService.getMessages(sessionId).stream()
                        .map(ConversationMessageResponse::from)
                        .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}