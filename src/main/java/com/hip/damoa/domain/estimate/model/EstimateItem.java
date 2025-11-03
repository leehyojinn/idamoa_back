package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 견적 항목 상세
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_items", indexes = {
    @Index(name = "idx_estimate_items_proposal_id", columnList = "proposal_id")
})
public class EstimateItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private EstimateProposal proposal;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        if (this.quantity != null && this.unitPrice != null) {
            this.subtotal = this.quantity.multiply(this.unitPrice);
        }
    }

    // ===== Business Methods =====

    /**
     * 수량 업데이트
     */
    public void updateQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    /**
     * 단가 업데이트
     */
    public void updateUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
