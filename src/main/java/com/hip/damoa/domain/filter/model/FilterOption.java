package com.hip.damoa.domain.filter.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 필터 옵션
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "filter_options", indexes = {
    @Index(name = "idx_filter_options_category_id", columnList = "category_id"),
    @Index(name = "idx_filter_options_code", columnList = "code")
})
public class FilterOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FilterCategory category;

    @Column(name = "code", nullable = false, length = 50)
    private String optionCode;

    @Column(name = "name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "short_name", length = 100)
    private String optionValue;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "icon", length = 100)
    private String iconUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    public void incrementUsageCount() {
        this.usageCount++;
    }

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
