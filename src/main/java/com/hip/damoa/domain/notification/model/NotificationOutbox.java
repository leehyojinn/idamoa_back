package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 발송 Outbox 엔티티
 *
 * Transactional Outbox Pattern을 사용하여 알림을 안정적으로 발송
 * - 트랜잭션과 함께 Outbox에 기록
 * - 별도 워커가 폴링하여 실제 발송
 * - 재시도 및 지수 백오프 지원
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_notification_outbox_status", columnList = "status, scheduled_at"),
        @Index(name = "idx_notification_outbox_debounce", columnList = "debounce_key, status"),
        @Index(name = "idx_notification_outbox_recipient", columnList = "recipient_id, status")
})
public class NotificationOutbox extends BaseTimeEntity {

    /**
     * 수신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User recipient;

    /**
     * 수신자 전화번호 (별도 저장 - 사용자 정보 변경 대비)
     */
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    /**
     * 알림 채널
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /**
     * 템플릿 코드 (카카오 알림톡용)
     */
    @Column(name = "template_code", length = 50)
    private String templateCode;

    /**
     * 제목
     */
    @Column(name = "title", length = 200)
    private String title;

    /**
     * 내용
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 템플릿 데이터 (변수 치환용)
     */
    @Type(JsonBinaryType.class)
    @Column(name = "template_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> templateData = new HashMap<>();

    /**
     * 관련 엔티티 타입
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * 관련 엔티티 ID
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * 발송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    /**
     * 발송 예정 시간
     */
    @Column(name = "scheduled_at", nullable = false)
    @Builder.Default
    private LocalDateTime scheduledAt = LocalDateTime.now();

    /**
     * 실제 처리 시간
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 최대 재시도 횟수
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 마지막 에러 메시지
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * 디바운스 키 (중복 발송 방지)
     */
    @Column(name = "debounce_key", length = 200)
    private String debounceKey;

    /**
     * 채팅 메시지 알림 생성
     */
    public static NotificationOutbox createChatNotification(
            User recipient,
            String senderName,
            String messagePreview,
            Long roomId) {

        String content = String.format("%s님이 메시지를 보냈습니다: %s",
                senderName,
                messagePreview.length() > 50 ? messagePreview.substring(0, 50) + "..." : messagePreview);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("senderName", senderName);
        templateData.put("messagePreview", messagePreview);
        templateData.put("roomId", roomId);

        return NotificationOutbox.builder()
                .recipient(recipient)
                .recipientPhone(null) // UserProfile에서 별도 조회 필요
                .channel(NotificationChannel.KAKAO)
                .notificationType(NotificationType.CHAT_MESSAGE)
                .title("새 메시지")
                .content(content)
                .templateData(templateData)
                .entityType("DirectChatRoom")
                .entityId(roomId)
                .debounceKey(String.format("chat:room:%d:user:%d", roomId, recipient.getId()))
                .build();
    }

    /**
     * 처리 시작
     */
    public void startProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    /**
     * 발송 성공
     */
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 (재시도 가능)
     */
    public void markFailed(String error) {
        this.lastError = error;
        this.retryCount++;

        if (this.retryCount >= this.maxRetries) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.status = OutboxStatus.PENDING;
            // 지수 백오프: 1분, 2분, 4분...
            int delayMinutes = (int) Math.pow(2, this.retryCount - 1);
            this.scheduledAt = LocalDateTime.now().plusMinutes(delayMinutes);
        }
    }

    /**
     * 취소
     */
    public void cancel() {
        this.status = OutboxStatus.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 재시도 가능 여부
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }
}
