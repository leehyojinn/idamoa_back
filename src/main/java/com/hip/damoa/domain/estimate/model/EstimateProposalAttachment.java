package com.hip.damoa.domain.estimate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 견적 제안 첨부파일 조인 테이블
 * Company 패턴(CompanyImage)과 동일한 방식으로 파일 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_proposal_attachments", indexes = {
    @Index(name = "idx_estimate_proposal_attachments_proposal", columnList = "estimate_proposal_id"),
    @Index(name = "idx_estimate_proposal_attachments_file", columnList = "file_id"),
    @Index(name = "idx_estimate_proposal_attachments_order", columnList = "estimate_proposal_id, display_order")
})
public class EstimateProposalAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_proposal_id", nullable = false)
    private EstimateProposal estimateProposal;

    @Column(name = "file_id", nullable = false)
    private Long fileId;  // File ID (FK to files.id)

    @Column(name = "file_type", length = 50)
    private String fileType; // DRAWING, PHOTO, DOCUMENT, ESTIMATE, etc.

    @Column(name = "file_description", columnDefinition = "TEXT")
    private String fileDescription;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Lifecycle Callbacks =====

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Business Methods =====

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }

    /**
     * 파일 설명 수정
     */
    public void updateDescription(String description) {
        this.fileDescription = description;
    }

    /**
     * 파일 타입 수정
     */
    public void updateFileType(String fileType) {
        this.fileType = fileType;
    }
}
