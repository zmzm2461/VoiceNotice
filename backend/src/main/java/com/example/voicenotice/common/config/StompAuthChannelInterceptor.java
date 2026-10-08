package com.example.voicenotice.common.config;

import com.example.voicenotice.auth.jwt.JwtTokenProvider;
import com.example.voicenotice.device.repository.DevicePairingRepository;
import com.example.voicenotice.session.entity.IntercomSession;
import com.example.voicenotice.session.repository.IntercomSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final IntercomSessionRepository intercomSessionRepository;
    private final DevicePairingRepository devicePairingRepository;

    private static final Pattern SESSION_TOPIC_PATTERN =
            Pattern.compile("^/topic/sessions/(\\d+)(?:/.*)?$");

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }

        return message;
    }


    private void authenticate(StompHeaderAccessor accessor) {

        String authorization =
                accessor.getFirstNativeHeader("Authorization");

        // 혹시 프론트/라이브러리에서 소문자로 들어오는 경우도 허용
        if (authorization == null) {
            authorization =
                    accessor.getFirstNativeHeader("authorization");
        }

        System.out.println(
                "[STOMP CONNECT] native header keys = "
                        + accessor.toNativeHeaderMap().keySet()
        );

        System.out.println(
                "[STOMP CONNECT] Authorization exists = "
                        + (authorization != null)
        );

        if (authorization != null) {
            System.out.println(
                    "[STOMP CONNECT] Bearer format = "
                            + authorization.startsWith("Bearer ")
            );

            System.out.println(
                    "[STOMP CONNECT] Authorization length = "
                            + authorization.length()
            );
        }

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            throw new MessageDeliveryException(
                    "로그인이 필요합니다."
            );
        }

        String token = authorization.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new MessageDeliveryException(
                    "유효하지 않은 토큰입니다."
            );
        }

        Long userId =
                jwtTokenProvider.getUserId(token);

        String role =
                jwtTokenProvider.getRole(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        )
                );

        accessor.setUser(authentication);

        System.out.println(
                "[STOMP CONNECT] 인증 성공 userId=" + userId
        );
    }


    private void authorizeSubscription(
            StompHeaderAccessor accessor
    ) {

        String destination =
                accessor.getDestination();

        if (destination == null) {
            return;
        }

        Matcher matcher =
                SESSION_TOPIC_PATTERN.matcher(destination);

        /*
         * 세션 관련 topic이 아니면 여기서는 검사하지 않음
         */
        if (!matcher.matches()) {
            return;
        }

        if (accessor.getUser() == null) {
            throw new MessageDeliveryException(
                    "로그인이 필요합니다."
            );
        }

        Long userId;

        try {
            userId = Long.valueOf(
                    accessor.getUser().getName()
            );
        } catch (Exception e) {
            throw new MessageDeliveryException(
                    "사용자 인증 정보를 확인할 수 없습니다."
            );
        }

        Long sessionId =
                Long.valueOf(matcher.group(1));

        /*
         * sessionId가 실제 존재하는지 확인
         */
        IntercomSession session =
                intercomSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new MessageDeliveryException(
                                        "세션을 찾을 수 없습니다."
                                )
                        );

        Long deviceId =
                session.getDevice().getId();

        /*
         * 로그인 사용자와
         * 해당 인터폰 기기가 현재 페어링되어 있는지 확인
         */
        boolean hasAccess =
                devicePairingRepository
                        .existsByDevice_IdAndUser_IdAndUnpairedAtIsNull(
                                deviceId,
                                userId
                        );

        if (!hasAccess) {
            throw new MessageDeliveryException(
                    "해당 세션에 대한 권한이 없습니다."
            );
        }
    }
}