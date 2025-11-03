package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 어드민 알림
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_notifications", indexes = {
    @Index(name = "idx_admin_notifications_admin_user_id", columnList = "admin_user_id"),
    @Index(name = "idx_admin_notifications_type", columnList = "notification_type"),
    @Index(name = "idx_admin_notifications_status", columnList = "is_read"),
    @Index(name = "idx_admin_notifications_created_at", columnList = "created_at")
})
public class AdminNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser; // null = broadcast to all admins

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType; // ALERT, WARNING, INFO, REPORT, etc.

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Type(JsonBinaryType.class)
    @Column(name = "action_data", columnDefinition = "jsonb")
    private Map<String, Object> actionData;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
