package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verifications", indexes = {
    @Index(name = "idx_email_verifications_email", columnList = "email"),
    @Index(name = "idx_email_verifications_token", columnList = "verification_token"),
    @Index(name = "idx_email_verifications_status", columnList = "status")
})
public class EmailVerification extends BaseEntity {

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "verification_token", unique = true, nullable = false)
    private String verificationToken;

    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose; // SIGNUP, FIND_PASSWORD, CHANGE_EMAIL

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, VERIFIED, EXPIRED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "verified_ip", length = 45)
    private String verifiedIp;

    @PrePersist
    protected void generateToken() {
        if (this.verificationToken == null) {
            this.verificationToken = UUID.randomUUID().toString();
        }
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean verify(String token) {
        if (isExpired()) {
            this.status = "EXPIRED";
            return false;
        }

        if (this.verificationToken.equals(token)) {
            this.status = "VERIFIED";
            this.verifiedAt = LocalDateTime.now();
            return true;
        }

        return false;
    }

    public void resend() {
        this.verificationToken = UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.status = "PENDING";
    }

    public void markAsVerified(String verifiedIp) {
        this.status = "VERIFIED";
        this.verifiedAt = LocalDateTime.now();
        this.verifiedIp = verifiedIp;
    }
}
