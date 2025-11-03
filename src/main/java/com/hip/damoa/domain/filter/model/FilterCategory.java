package com.hip.damoa.domain.filter.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 필터 카테고리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "filter_categories", indexes = {
    @Index(name = "idx_filter_categories_code", columnList = "category_code"),
    @Index(name = "idx_filter_categories_type", columnList = "filter_type")
})
public class FilterCategory extends BaseEntity {

    @Column(name = "category_code", unique = true, nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "filter_type", nullable = false, length = 20)
    private String filterType; // SINGLE_SELECT, MULTI_SELECT, RANGE, SEARCH

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
