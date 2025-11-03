package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 업체 제공 서비스
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_services", indexes = {
    @Index(name = "idx_company_services_company_id", columnList = "company_id"),
    @Index(name = "idx_company_services_category", columnList = "category")
})
public class CompanyService extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "price_min", precision = 12, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", precision = 12, scale = 2)
    private BigDecimal priceMax;

    @Column(name = "price_unit", length = 50)
    private String priceUnit; // PER_HOUR, PER_PROJECT, PER_SQFT 등

    @Column(name = "duration_days")
    private Integer durationDays;

    @Type(JsonBinaryType.class)
    @Column(name = "service_options", columnDefinition = "jsonb")
    private Map<String, Object> serviceOptions;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    // ===== Business Methods =====

    /**
     * 서비스 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 서비스 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 가격 업데이트
     */
    public void updatePrice(BigDecimal minPrice, BigDecimal maxPrice, String unit) {
        this.priceMin = minPrice;
        this.priceMax = maxPrice;
        this.priceUnit = unit;
    }

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
