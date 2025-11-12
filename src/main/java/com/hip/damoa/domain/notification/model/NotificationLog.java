package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 발송 로그 (감사 로그 - 절대 삭제하면 안됨)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notification_logs_notification_id", columnList = "notification_id"),
    @Index(name = "idx_notification_logs_status", columnList = "status"),
    @Index(name = "idx_notification_logs_created_at", columnList = "created_at")
})
public class NotificationLog extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private NotificationChannel channel; // EMAIL, SMS, KAKAO, FCM

    @Column(name = "recipient", length = 255)
    private String recipient;

    @Column(name = "provider", length = 50)
    private String provider; // GMAIL_API, AWS_SES, SENS, FCM, KAKAO

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, SENT, FAILED, DELIVERED

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Type(JsonBinaryType.class)
    @Column(name = "request_data", columnDefinition = "jsonb")
    private Map<String, Object> requestData;

    @Type(JsonBinaryType.class)
    @Column(name = "response_data", columnDefinition = "jsonb")
    private Map<String, Object> responseData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    public void markAsSent() {
        this.status = "SENT";
        this.sentAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.status = "DELIVERED";
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.failedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}
