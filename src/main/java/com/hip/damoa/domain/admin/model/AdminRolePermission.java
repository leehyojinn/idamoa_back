package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 어드민 역할-권한 매핑
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_role_permissions", indexes = {
    @Index(name = "idx_admin_role_permissions_role_id", columnList = "role_id"),
    @Index(name = "idx_admin_role_permissions_permission_id", columnList = "permission_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
})
public class AdminRolePermission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private AdminPermission permission;

    @Column(name = "granted_by")
    private Long grantedBy; // Admin user ID who granted this permission
}
