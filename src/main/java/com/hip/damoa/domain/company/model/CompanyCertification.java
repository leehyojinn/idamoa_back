package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 업체 인증서/자격증
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_certifications", indexes = {
    @Index(name = "idx_company_certifications_company_id", columnList = "company_id"),
    @Index(name = "idx_company_certifications_status", columnList = "status")
})
public class CompanyCertification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "issuer", length = 200)
    private String issuer;

    @Column(name = "certification_number", length = 100)
    private String certificationNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "certificate_image_url", length = 500)
    private String certificateImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, VERIFIED, REJECTED, EXPIRED

    @Column(name = "verified_by")
    private Long verifiedBy; // admin user id

    @Column(name = "verified_at")
    private java.time.LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ===== Business Methods =====

    /**
     * 인증서 승인
     */
    public void verify(Long adminId) {
        this.status = "VERIFIED";
        this.verifiedBy = adminId;
        this.verifiedAt = java.time.LocalDateTime.now();
        this.rejectionReason = null;
    }

    /**
     * 인증서 거절
     */
    public void reject(Long adminId, String reason) {
        this.status = "REJECTED";
        this.verifiedBy = adminId;
        this.verifiedAt = java.time.LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * 만료 처리
     */
    public void markAsExpired() {
        this.status = "EXPIRED";
    }

    /**
     * 인증서 갱신
     */
    public void renew(LocalDate newExpiryDate) {
        this.expiryDate = newExpiryDate;
        if (this.status.equals("EXPIRED")) {
            this.status = "VERIFIED";
        }
    }
}
