package com.hip.damoa.domain.notification.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.web.dto.NotificationListResponse;
import com.hip.damoa.domain.notification.web.dto.NotificationResponse;
import com.hip.damoa.domain.notification.web.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * WebSocket 양방향 통신 컨트롤러
 *
 * 클라이언트 → 서버 메시지 처리
 * - 알림 읽음 처리
 * - 알림 목록 조회
 * - 미읽음 개수 조회
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationWebSocketController {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 읽음 처리
     *
     * 클라이언트 → 서버: /app/notifications/{uuid}/read
     * 서버 → 클라이언트: /user/queue/reply
     *
     * @param uuid 알림 UUID
     * @param principal 인증된 사용자 (JWT에서 추출된 이메일)
     */
    @MessageMapping("/notifications/{uuid}/read")
    @SendToUser("/queue/reply")
    public NotificationResponse markAsRead(
            @DestinationVariable UUID uuid,
            Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 알림 읽음 처리: uuid={}, userEmail={}", uuid, userEmail);

        Notification notification = notificationRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getRecipientEmail().equals(userEmail)) {
            log.warn("알림 접근 권한 없음: uuid={}, userEmail={}, recipientEmail={}",
                    uuid, userEmail, notification.getRecipientEmail());
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 읽음 처리
        notification.markAsRead();
        notificationRepository.save(notification);

        log.info("WebSocket 알림 읽음 처리 완료: uuid={}", uuid);
        return NotificationResponse.from(notification);
    }

    /**
     * 알림 목록 조회
     *
     * 클라이언트 → 서버: /app/notifications/list
     * 서버 → 클라이언트: /user/queue/notifications-list
     *
     * @param principal 인증된 사용자
     */
    @MessageMapping("/notifications/list")
    @SendToUser("/queue/notifications-list")
    public NotificationListResponse getNotifications(Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 알림 목록 조회: userEmail={}", userEmail);

        List<Notification> notifications = notificationRepository
                .findByRecipientEmailAndIsDeletedFalseOrderByCreatedAtDesc(userEmail);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();

        log.info("WebSocket 알림 목록 조회 완료: userEmail={}, count={}", userEmail, responses.size());
        return NotificationListResponse.builder()
                .notifications(responses)
                .totalCount(responses.size())
                .build();
    }

    /**
     * 미읽음 알림 개수 조회
     *
     * 클라이언트 → 서버: /app/notifications/unread-count
     * 서버 → 클라이언트: /user/queue/unread-count
     *
     * @param principal 인증된 사용자
     */
    @MessageMapping("/notifications/unread-count")
    @SendToUser("/queue/unread-count")
    public UnreadCountResponse getUnreadCount(Principal principal) {

        String userEmail = principal.getName();
        log.info("WebSocket 미읽음 개수 조회: userEmail={}", userEmail);

        long unreadCount = notificationRepository
                .countByRecipientEmailAndIsReadFalseAndIsDeletedFalse(userEmail);

        log.info("WebSocket 미읽음 개수 조회 완료: userEmail={}, count={}", userEmail, unreadCount);
        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }
}
