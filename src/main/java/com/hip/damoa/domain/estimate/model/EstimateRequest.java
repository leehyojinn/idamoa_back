package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 견적 요청
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_requests", indexes = {
    @Index(name = "idx_estimate_requests_user_id", columnList = "user_id"),
    @Index(name = "idx_estimate_requests_status", columnList = "status"),
    @Index(name = "idx_estimate_requests_created_at", columnList = "created_at")
})
public class EstimateRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private Map<String, Object> requirements;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Type(StringArrayType.class)
    @Column(name = "required_skills", columnDefinition = "text[]")
    private String[] requiredSkills;

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "budget_type", length = 20)
    private String budgetType; // FIXED, NEGOTIABLE, HOURLY

    @Column(name = "preferred_start_date")
    private LocalDate preferredStartDate;

    @Column(name = "expected_duration_days")
    private Integer expectedDurationDays;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED

    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private String visibility = "PUBLIC"; // PUBLIC, PRIVATE, INVITED_ONLY

    @Column(name = "proposal_count", nullable = false)
    @Builder.Default
    private Integer proposalCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt;

    @Column(name = "deadline")
    private java.time.LocalDateTime deadline;

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;

    // ===== Business Methods =====

    /**
     * 견적 요청 발행
     */
    public void publish() {
        this.status = "PUBLISHED";
        this.publishedAt = java.time.LocalDateTime.now();
    }

    /**
     * 진행 중으로 변경
     */
    public void startProgress() {
        this.status = "IN_PROGRESS";
    }

    /**
     * 매칭 완료
     */
    public void match() {
        this.status = "MATCHED";
    }

    /**
     * 완료 처리
     */
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = java.time.LocalDateTime.now();
    }

    /**
     * 취소 처리
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * 제안 수 증가
     */
    public void incrementProposalCount() {
        this.proposalCount++;
    }

    /**
     * 제안 수 감소
     */
    public void decrementProposalCount() {
        if (this.proposalCount > 0) {
            this.proposalCount--;
        }
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
}
