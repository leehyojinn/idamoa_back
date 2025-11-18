package com.hip.damoa.config.web;

import com.hip.damoa.core.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket JWT 인증 인터셉터
 *
 * WebSocket 연결 시 JWT 토큰을 검증하여 사용자 Principal 설정
 *
 * 토큰 전달 방법:
 * 1. Authorization header: "Bearer {token}"
 * 2. Query parameter: "?token={token}"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    accessor.setUser(authentication);
                    log.info("WebSocket 인증 성공: user={}", authentication.getName());
                } catch (Exception e) {
                    log.error("WebSocket 인증 실패: error={}", e.getMessage(), e);
                }
            } else {
                log.warn("WebSocket 인증 실패: 유효하지 않은 토큰");
            }
        }

        return message;
    }

    /**
     * JWT 토큰 추출
     *
     * 1. Authorization header에서 Bearer 토큰 추출
     * 2. Query parameter "token"에서 추출
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Authorization header에서 추출
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.debug("Authorization header에서 토큰 추출: {}", token.substring(0, Math.min(20, token.length())) + "...");
                return token;
            }
        }

        // 2. Query parameter에서 추출 (Native header에 포함됨)
        // SockJS/STOMP에서는 handshake 시 query parameter가 native header로 전달됨
        List<String> tokenParams = accessor.getNativeHeader("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            String token = tokenParams.get(0);
            log.debug("Query parameter에서 토큰 추출: {}", token.substring(0, Math.min(20, token.length())) + "...");
            return token;
        }

        log.warn("토큰을 찾을 수 없습니다");
        return null;
    }
}
