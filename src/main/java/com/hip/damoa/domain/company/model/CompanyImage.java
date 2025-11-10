package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 업체 복수 이미지 관리 (V2 추가)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_images", indexes = {
    @Index(name = "idx_company_images_company_id", columnList = "company_id"),
    @Index(name = "idx_company_images_type", columnList = "image_type")
})
public class CompanyImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "file_id", nullable = false)
    private Long fileId;  // File ID (FK to files.id)

    @Column(name = "image_type", nullable = false, length = 20)
    private String imageType; // LOGO, COVER, GALLERY, INTERIOR, EXTERIOR, PORTFOLIO

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    // ===== Business Methods =====

    /**
     * 대표 이미지 설정
     */
    public void setPrimary() {
        this.isPrimary = true;
    }

    /**
     * 대표 이미지 해제
     */
    public void unsetPrimary() {
        this.isPrimary = false;
    }

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
