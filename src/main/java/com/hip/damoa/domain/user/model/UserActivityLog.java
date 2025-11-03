package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 사용자 활동 로그
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_activity_logs", indexes = {
    @Index(name = "idx_user_activity_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_user_activity_logs_event_type", columnList = "event_type"),
    @Index(name = "idx_user_activity_logs_created_at", columnList = "created_at")
})
public class UserActivityLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Type(JsonBinaryType.class)
    @Column(name = "activity_data", columnDefinition = "jsonb")
    private Map<String, Object> activityData;
}
