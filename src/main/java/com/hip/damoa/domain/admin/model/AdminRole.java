package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 어드민 역할
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_roles", indexes = {
    @Index(name = "idx_admin_roles_code", columnList = "role_code")
})
public class AdminRole extends BaseEntity {

    @Column(name = "role_code", unique = true, nullable = false, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false)
    private Integer priority; // Higher number = higher priority

    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private Boolean isSystemRole = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        if (!this.isSystemRole) {
            this.isActive = false;
        }
    }
}
