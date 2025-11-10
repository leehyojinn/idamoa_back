package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    @Index(name = "idx_estimate_proposals_status", columnList = "status"),
    @Index(name = "idx_estimate_proposals_is_selected", columnList = "is_selected")
})
public class EstimateProposal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private EstimateRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // ===== 핵심 필드 =====

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    // ===== 첨부파일 (V30 Migration: 조인 테이블로 변경) =====

    @OneToMany(mappedBy = "estimateProposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EstimateProposalAttachment> attachments = new ArrayList<>();

    // ===== 상태 관리 =====

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "SUBMITTED"; // DRAFT, SUBMITTED, VIEWED, SELECTED, REJECTED, WITHDRAWN

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private Boolean isSelected = false;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    // ===== 추가 정보 =====

    @Column(name = "valid_until")
    private LocalDate validUntil;  // 제안 유효기간

    @Type(JsonBinaryType.class)
    @Column(name = "pricing_details", columnDefinition = "jsonb")
    private Map<String, Object> pricingDetails;  // 가격 상세 정보

    @Type(JsonBinaryType.class)
    @Column(name = "timeline", columnDefinition = "jsonb")
    private Map<String, Object> timeline;  // 일정 정보

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;  // 기타 메타데이터

    // ===== Business Methods =====

    /**
     * 제안 확인 처리
     */
    public void markAsViewed() {
        if ("SUBMITTED".equals(this.status)) {
            this.status = "VIEWED";
        }
    }

    /**
     * 제안 선택
     */
    public void select() {
        this.status = "SELECTED";
        this.isSelected = true;
        this.selectedAt = LocalDateTime.now();
    }

    /**
     * 제안 거절
     */
    public void reject(String reason) {
        this.status = "REJECTED";
        this.isSelected = false;
    }

    /**
     * 제안 철회
     */
    public void withdraw() {
        this.status = "WITHDRAWN";
    }

    /**
     * 제안이 유효한지 확인
     */
    public boolean isValid() {
        if (validUntil == null) {
            return true;
        }
        return LocalDate.now().isBefore(validUntil) || LocalDate.now().isEqual(validUntil);
    }

    /**
     * 제안이 선택 가능한 상태인지 확인
     */
    public boolean isSelectable() {
        return "VIEWED".equals(this.status) && !this.isSelected && isValid();
    }

    // ===== 첨부파일 관리 메서드 =====

    /**
     * 첨부파일 추가
     */
    public void addAttachment(EstimateProposalAttachment attachment) {
        this.attachments.add(attachment);
    }

    /**
     * 첨부파일 제거
     */
    public void removeAttachment(EstimateProposalAttachment attachment) {
        this.attachments.remove(attachment);
    }

    /**
     * 모든 첨부파일 제거
     */
    public void clearAttachments() {
        this.attachments.clear();
    }
}