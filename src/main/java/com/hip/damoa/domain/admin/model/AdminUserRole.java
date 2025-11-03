package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 어드민 사용자-역할 매핑
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_user_roles", indexes = {
    @Index(name = "idx_admin_user_roles_user_id", columnList = "admin_user_id"),
    @Index(name = "idx_admin_user_roles_role_id", columnList = "role_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_role", columnNames = {"admin_user_id", "role_id"})
})
public class AdminUserRole extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    @Column(name = "granted_by")
    private Long grantedBy; // Admin user ID who granted this role

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
