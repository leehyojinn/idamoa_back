package com.hip.damoa.domain.planner.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 플래너 신청서 첨부파일 엔티티
 * EstimateRequestAttachment와 동일한 패턴
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "planner_application_attachments", indexes = {
    @Index(name = "idx_planner_application_attachments_application", columnList = "planner_application_id"),
    @Index(name = "idx_planner_application_attachments_file", columnList = "file_id"),
    @Index(name = "idx_planner_application_attachments_order", columnList = "planner_application_id, display_order")
})
public class PlannerApplicationAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_application_id", nullable = false)
    private PlannerApplication plannerApplication;

    @Column(name = "file_id", nullable = false)
    private Long fileId;  // File ID (FK to files.id)

    @Column(name = "file_type", length = 50)
    private String fileType; // DRAWING, PHOTO, DOCUMENT 등

    @Column(name = "file_description", columnDefinition = "TEXT")
    private String fileDescription;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
     * PlannerApplication 설정 (양방향 관계)
     */
    public void setPlannerApplication(PlannerApplication plannerApplication) {
        this.plannerApplication = plannerApplication;
    }

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
