package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SMS 인증
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sms_verifications", indexes = {
    @Index(name = "idx_sms_verifications_phone", columnList = "phone_number"),
    @Index(name = "idx_sms_verifications_status", columnList = "status")
})
public class SmsVerification extends BaseEntity {

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "verification_code", nullable = false, length = 10)
    private String verificationCode;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose; // SIGNUP, FIND_PASSWORD, CHANGE_PHONE, IDENTITY_VERIFY

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, VERIFIED, EXPIRED, FAILED

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void setExpiration() {
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusMinutes(5);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean verify(String code) {
        this.attemptCount++;

        if (isExpired()) {
            this.status = "EXPIRED";
            return false;
        }

        if (this.attemptCount > 5) {
            this.status = "FAILED";
            return false;
        }

        if (this.verificationCode.equals(code)) {
            this.status = "VERIFIED";
            this.verifiedAt = LocalDateTime.now();
            return true;
        }

        return false;
    }
}
