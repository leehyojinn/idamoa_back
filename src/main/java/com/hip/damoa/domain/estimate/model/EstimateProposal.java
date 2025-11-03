package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 견적 제안
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_proposals", indexes = {
    @Index(name = "idx_estimate_proposals_request_id", columnList = "request_id"),
    @Index(name = "idx_estimate_proposals_company_id", columnList = "company_id"),
    @Index(name = "idx_estimate_proposals_status", columnList = "status")
})
public class EstimateProposal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private EstimateRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "proposal_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal proposalAmount;

    @Column(name = "proposal_content", columnDefinition = "TEXT", nullable = false)
    private String proposalContent;

    @Column(name = "estimated_duration_days")
    private Integer estimatedDurationDays;

    @Column(name = "proposed_start_date")
    private LocalDate proposedStartDate;

    @Column(name = "proposed_end_date")
    private LocalDate proposedEndDate;

    @Type(StringArrayType.class)
    @Column(name = "portfolio_links", columnDefinition = "text[]")
    private String[] portfolioLinks;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUBMITTED"; // SUBMITTED, VIEWED, ACCEPTED, REJECTED, WITHDRAWN

    @Column(name = "viewed_at")
    private java.time.LocalDateTime viewedAt;

    @Column(name = "accepted_at")
    private java.time.LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private java.time.LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ===== Business Methods =====

    /**
     * 제안 확인 처리
     */
    public void markAsViewed() {
        if (this.status.equals("SUBMITTED")) {
            this.status = "VIEWED";
            this.viewedAt = java.time.LocalDateTime.now();
        }
    }

    /**
     * 제안 수락
     */
    public void accept() {
        this.status = "ACCEPTED";
        this.acceptedAt = java.time.LocalDateTime.now();
    }

    /**
     * 제안 거절
     */
    public void reject(String reason) {
        this.status = "REJECTED";
        this.rejectedAt = java.time.LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * 제안 철회
     */
    public void withdraw() {
        this.status = "WITHDRAWN";
    }
}
