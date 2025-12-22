package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 포트폴리오 프로모션 타입 설정
 * - 관리자가 동적으로 타입 추가/수정 가능
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_promotion_type_settings", indexes = {
    @Index(name = "idx_portfolio_promotion_type_settings_active", columnList = "is_active, display_order")
})
public class PortfolioPromotionTypeSetting extends BaseTimeEntity {

    @Column(name = "promotion_type", nullable = false, unique = true, length = 30)
    private String promotionType;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Integer weight = 1;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    // ===== Business Methods =====

    /**
     * 설정 업데이트
     */
    public void update(String displayName, BigDecimal price, Integer weight,
                       Integer displayOrder, Boolean isActive, String description, User updatedBy) {
        if (displayName != null) this.displayName = displayName;
        if (price != null) this.price = price;
        if (weight != null) this.weight = weight;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (isActive != null) this.isActive = isActive;
        if (description != null) this.description = description;
        this.updatedBy = updatedBy;
    }

    /**
     * 비활성화
     */
    public void deactivate(User admin) {
        this.isActive = false;
        this.updatedBy = admin;
    }

    /**
     * 활성화
     */
    public void activate(User admin) {
        this.isActive = true;
        this.updatedBy = admin;
    }
}
