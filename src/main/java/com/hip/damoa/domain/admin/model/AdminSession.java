package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 어드민 세션
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_sessions", indexes = {
    @Index(name = "idx_admin_sessions_admin_user_id", columnList = "admin_user_id"),
    @Index(name = "idx_admin_sessions_token", columnList = "session_token"),
    @Index(name = "idx_admin_sessions_status", columnList = "status")
})
public class AdminSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, REVOKED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void revoke() {
        this.status = "REVOKED";
    }

    public void markAsExpired() {
        this.status = "EXPIRED";
    }
}
