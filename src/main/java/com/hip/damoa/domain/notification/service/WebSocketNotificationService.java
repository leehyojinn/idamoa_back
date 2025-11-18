package com.hip.damoa.domain.notification.service;

import com.hip.damoa.domain.notification.web.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 알림 전송 서비스
 *
 * STOMP 프로토콜을 사용하여 특정 사용자에게 실시간 알림 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 사용자에게 알림 전송
     *
     * @param userEmail 수신자 이메일 (WebSocket 인증 시 사용된 principal)
     * @param message 알림 메시지
     *
     * 전송 경로: /user/{userEmail}/queue/notifications
     * 클라이언트 구독: /user/queue/notifications
     */
    public void sendToUser(String userEmail, NotificationMessage message) {
        try {
            log.info("WebSocket 알림 전송: userEmail={}, type={}, uuid={}",
                    userEmail, message.getType(), message.getUuid());

            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/notifications",
                    message
            );

            log.debug("WebSocket 알림 전송 성공");
        } catch (Exception e) {
            log.error("WebSocket 알림 전송 실패: userEmail={}, error={}",
                    userEmail, e.getMessage(), e);
            // WebSocket 전송 실패해도 DB에는 저장되므로 예외를 던지지 않음
        }
    }

    /**
     * 특정 토픽에 브로드캐스트 (전체 구독자에게 전송)
     *
     * @param topic 토픽 경로 (예: /topic/announcements)
     * @param message 알림 메시지
     */
    public void sendToTopic(String topic, NotificationMessage message) {
        try {
            log.info("WebSocket 브로드캐스트: topic={}, type={}",
                    topic, message.getType());

            messagingTemplate.convertAndSend(topic, message);

            log.debug("WebSocket 브로드캐스트 성공");
        } catch (Exception e) {
            log.error("WebSocket 브로드캐스트 실패: topic={}, error={}",
                    topic, e.getMessage(), e);
        }
    }
}
