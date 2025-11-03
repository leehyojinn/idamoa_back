package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 어드민 감사 로그
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_audit_logs", indexes = {
    @Index(name = "idx_admin_audit_logs_admin_user_id", columnList = "admin_user_id"),
    @Index(name = "idx_admin_audit_logs_action", columnList = "action"),
    @Index(name = "idx_admin_audit_logs_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_admin_audit_logs_created_at", columnList = "created_at")
})
public class AdminAuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, APPROVE, REJECT, LOGIN, LOGOUT, etc.

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes; // Before/After values

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "is_successful", nullable = false)
    @Builder.Default
    private Boolean isSuccessful = true;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
