package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 어드민 권한
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_permissions", indexes = {
    @Index(name = "idx_admin_permissions_code", columnList = "permission_code"),
    @Index(name = "idx_admin_permissions_resource", columnList = "resource_type")
})
public class AdminPermission extends BaseEntity {

    @Column(name = "permission_code", unique = true, nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType; // USER, COMPANY, PAYMENT, AD, ESTIMATE, etc.

    @Column(name = "action", nullable = false, length = 20)
    private String action; // CREATE, READ, UPDATE, DELETE, APPROVE, etc.

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system_permission", nullable = false)
    @Builder.Default
    private Boolean isSystemPermission = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        if (!this.isSystemPermission) {
            this.isActive = false;
        }
    }
}
