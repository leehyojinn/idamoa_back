package com.hip.damoa.domain.admin.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * 어드민 사용자
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_users", indexes = {
    @Index(name = "idx_admin_users_email", columnList = "email"),
    @Index(name = "idx_admin_users_status", columnList = "status")
})
public class AdminUser extends BaseEntity {

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Type(StringArrayType.class)
    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    private String[] roles; // SUPER_ADMIN, ADMIN, OPERATOR, VIEWER

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Long loginCount = 0L;

    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "is_2fa_enabled", nullable = false)
    @Builder.Default
    private Boolean is2faEnabled = false;

    @Column(name = "two_fa_secret", length = 200)
    private String twoFaSecret;

    public void loginSuccess(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.loginCount++;
        this.failedLoginCount = 0;
    }

    public void loginFailed() {
        this.failedLoginCount++;
        if (this.failedLoginCount >= 5) {
            this.status = "SUSPENDED";
        }
    }

    public void activate() {
        this.status = "ACTIVE";
        this.failedLoginCount = 0;
    }

    public void suspend() {
        this.status = "SUSPENDED";
    }

    public void enable2FA(String secret) {
        this.is2faEnabled = true;
        this.twoFaSecret = secret;
    }

    public void disable2FA() {
        this.is2faEnabled = false;
        this.twoFaSecret = null;
    }
}
