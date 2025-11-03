package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 쿠폰 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_code", columnList = "code", unique = true),
    @Index(name = "idx_coupons_status", columnList = "status")
})
public class Coupon extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    @Column(name = "discount_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_purchase_amount", precision = 12, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Type(JsonBinaryType.class)
    @Column(name = "usage_conditions", columnDefinition = "jsonb")
    private Map<String, Object> usageConditions;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "current_usage_count", nullable = false)
    @Builder.Default
    private Integer currentUsageCount = 0;

    @Column(name = "max_usage_per_user")
    private Integer maxUsagePerUser;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, EXPIRED

    public void incrementUsage() {
        this.currentUsageCount++;
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status.equals("ACTIVE") &&
               !now.isBefore(validFrom) &&
               !now.isAfter(validUntil) &&
               (maxUsageCount == null || currentUsageCount < maxUsageCount);
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void markAsExpired() {
        this.status = "EXPIRED";
    }
}
