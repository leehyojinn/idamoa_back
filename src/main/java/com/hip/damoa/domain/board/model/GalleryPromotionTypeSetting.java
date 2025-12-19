package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 갤러리 우대등록 타입별 설정 Entity
 * - 각 우대 타입(STANDARD, PREMIUM 등)별로 개별 row 관리
 * - 새로운 타입 추가 시 스키마 변경 없이 row만 추가
 */
@Entity
@Table(name = "gallery_promotion_type_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GalleryPromotionTypeSetting extends BaseTimeEntity {

    @Column(name = "promotion_type", nullable = false, unique = true, length = 30)
    private String promotionType;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "weight", nullable = false)
    private Integer weight = 1;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Builder
    public GalleryPromotionTypeSetting(String promotionType, String displayName,
                                        BigDecimal price, Integer weight,
                                        Integer displayOrder, String description) {
        this.promotionType = promotionType;
        this.displayName = displayName;
        this.price = price;
        this.weight = weight != null ? weight : 1;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = true;
        this.description = description;
    }

    /**
     * 설정 업데이트
     */
    public void update(String displayName, BigDecimal price, Integer weight,
                       Integer displayOrder, Boolean isActive, String description, User updatedBy) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (price != null) {
            this.price = price;
        }
        if (weight != null) {
            this.weight = weight;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedBy = updatedBy;
    }

    /**
     * 비활성화 (Soft Delete 대신 사용)
     */
    public void deactivate(User updatedBy) {
        this.isActive = false;
        this.updatedBy = updatedBy;
    }

    /**
     * 활성화
     */
    public void activate(User updatedBy) {
        this.isActive = true;
        this.updatedBy = updatedBy;
    }

    /**
     * GalleryPromotionType enum으로 변환
     */
    public GalleryPromotionType toEnumType() {
        try {
            return GalleryPromotionType.valueOf(this.promotionType);
        } catch (IllegalArgumentException e) {
            return null; // 새로운 타입은 enum에 없을 수 있음
        }
    }
}
