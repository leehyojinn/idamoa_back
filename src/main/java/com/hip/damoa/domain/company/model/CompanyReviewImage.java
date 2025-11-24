package com.hip.damoa.domain.company.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업체 리뷰 이미지 엔티티
 * File ID 기반으로 이미지 관리 (URL 직접 저장 X)
 */
@Entity
@Table(name = "company_review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CompanyReview review;

    /**
     * File ID (files 테이블 참조)
     * URL이 아닌 File ID로 저장하여 파일 관리 중앙화
     */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public CompanyReviewImage(CompanyReview review, Long fileId, Integer displayOrder) {
        this.review = review;
        this.fileId = fileId;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isDeleted = false;
    }

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
     * Soft Delete 처리
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 삭제 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
