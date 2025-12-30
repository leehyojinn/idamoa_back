package com.hip.damoa.domain.directchat.service;

import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import com.hip.damoa.domain.notification.model.NotificationOutbox;
import com.hip.damoa.domain.notification.repository.NotificationOutboxRepository;
import com.hip.damoa.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 채팅 알림 서비스 (Outbox 패턴 + 디바운스)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectChatNotificationService {

    private final NotificationOutboxRepository outboxRepository;
    private final DirectChatPresenceService presenceService;

    // 디바운스 TTL (초) - 5분
    private static final long DEBOUNCE_TTL_SECONDS = 300;

    /**
     * 채팅 메시지 알림 예약
     *
     * - 수신자가 채팅방을 보고 있으면 알림 발송 안함
     * - 디바운스 체크: 5분 내 중복 알림 방지
     * - Outbox에 저장하여 비동기 발송
     */
    @Transactional
    public void scheduleMessageNotification(DirectChatRoom room, DirectChatMessage message, User recipient) {
        log.debug("채팅 알림 예약 시작: roomId={}, messageId={}, recipientId={}",
                room.getId(), message.getId(), recipient.getId());

        // 수신자가 현재 채팅방을 보고 있으면 알림 불필요
        if (presenceService.isViewingRoom(recipient.getId(), room.getUuid())) {
            log.debug("수신자가 채팅방을 보고 있어 알림 생략: recipientId={}", recipient.getId());
            return;
        }

        // 디바운스 체크 (SETNX)
        boolean shouldSend = presenceService.checkAndSetDebounce(
                room.getId(), recipient.getId(), DEBOUNCE_TTL_SECONDS);

        if (!shouldSend) {
            log.debug("디바운스 중이므로 알림 생략: roomId={}, recipientId={}", room.getId(), recipient.getId());
            return;
        }

        // 발신자 정보 (User에는 nickname이 없으므로 email 사용)
        User sender = message.getSender();
        String senderName = "알 수 없음";
        if (sender != null && sender.getEmail() != null) {
            String email = sender.getEmail();
            // 이메일에서 @ 앞부분을 이름으로 사용
            senderName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        }

        // 메시지 미리보기
        String messagePreview = message.getContent();
        if (messagePreview == null || messagePreview.isEmpty()) {
            switch (message.getMessageType()) {
                case IMAGE:
                    messagePreview = "[이미지]";
                    break;
                case FILE:
                    messagePreview = "[파일]";
                    break;
                default:
                    messagePreview = "[메시지]";
            }
        }

        // Outbox에 알림 저장
        NotificationOutbox outbox = NotificationOutbox.createChatNotification(
                recipient, senderName, messagePreview, room.getId());
        outboxRepository.save(outbox);

        log.info("채팅 알림 예약 완료: outboxId={}, recipientId={}", outbox.getId(), recipient.getId());
    }

    /**
     * 사용자가 채팅방 입장 시 대기 중인 알림 취소
     */
    @Transactional
    public void cancelPendingNotifications(Long roomId, Long userId) {
        log.debug("대기 중인 알림 취소: roomId={}, userId={}", roomId, userId);

        // 디바운스 해제
        presenceService.clearDebounce(roomId, userId);

        // Outbox에서 대기 중인 알림 취소
        int cancelled = outboxRepository.cancelPendingByEntity(
                userId, "DirectChatRoom", roomId, LocalDateTime.now());

        if (cancelled > 0) {
            log.info("대기 중인 알림 {} 건 취소됨: roomId={}, userId={}", cancelled, roomId, userId);
        }
    }
}
