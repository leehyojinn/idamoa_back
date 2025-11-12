package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 알림 템플릿
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_notification_templates_channel", columnList = "channel"),
    @Index(name = "idx_notification_templates_code", columnList = "code")
})
public class NotificationTemplate extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel; // EMAIL, SMS, KAKAO, FCM

    @Column(name = "title_template", length = 500)
    private String titleTemplate;

    @Column(name = "content_template", columnDefinition = "TEXT", nullable = false)
    private String contentTemplate;

    @Type(JsonBinaryType.class)
    @Column(name = "variables", columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
