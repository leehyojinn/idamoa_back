package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 매칭 확정
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "matches", indexes = {
    @Index(name = "idx_matches_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_matches_proposal_id", columnList = "proposal_id", unique = true),
    @Index(name = "idx_matches_status", columnList = "status")
})
public class Match extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private EstimateRequest request;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private EstimateProposal proposal;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Type(JsonBinaryType.class)
    @Column(name = "contract_terms", columnDefinition = "jsonb")
    private Map<String, Object> contractTerms;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "CONFIRMED"; // CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "confirmed_at", nullable = false)
    @Builder.Default
    private LocalDateTime confirmedAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // ===== Business Methods =====

    /**
     * 프로젝트 시작
     */
    public void start() {
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 진행률 업데이트
     */
    public void updateProgress(Integer percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("진행률은 0-100 사이여야 합니다");
        }
        this.progressPercentage = percentage;
    }

    /**
     * 프로젝트 완료
     */
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
        this.actualEndDate = LocalDate.now();
    }

    /**
     * 프로젝트 취소
     */
    public void cancel(String reason) {
        this.status = "CANCELLED";
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }
}
