package com.hip.damoa.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정
 *
 * STOMP over WebSocket을 사용한 실시간 알림 시스템
 * - 견적 제안 도착 알림
 * - 상담 답변 도착 알림
 * - JWT 인증 지원
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * 메시지 브로커 설정
     *
     * SimpleBroker: 메모리 기반 메시지 브로커 (개발/소규모 운영용)
     * - /topic: 1:N 브로드캐스트 (전체 구독자에게 전송)
     * - /queue: 1:1 개인 메시지 (특정 사용자에게 전송)
     *
     * 운영 환경에서는 RabbitMQ, ActiveMQ 등 외부 브로커 사용 권장
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // SimpleBroker 활성화
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");

        // 사용자별 메시지 prefix (자동 추가됨)
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     *
     * 클라이언트가 WebSocket 연결을 시작하는 엔드포인트
     * - /ws: WebSocket 연결 URL
     * - SockJS fallback 지원 (WebSocket 미지원 브라우저 대응)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 허용 (운영 환경에서는 특정 도메인만 허용)
                .withSockJS();                   // SockJS fallback 활성화
    }

    /**
     * 클라이언트 인바운드 채널 설정
     *
     * JWT 인증 인터셉터 등록
     * - 클라이언트 → 서버 메시지 처리 전에 JWT 토큰 검증
     * - 유효한 토큰이면 Principal 설정
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
