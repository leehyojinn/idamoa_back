package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자 포인트 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_points", indexes = {
    @Index(name = "idx_user_points_user_id", columnList = "user_id", unique = true)
})
public class UserPoints extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_points", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal availablePoints = BigDecimal.ZERO;

    @Column(name = "pending_points", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal pendingPoints = BigDecimal.ZERO;

    @Column(name = "total_earned", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_spent", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "total_expired", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalExpired = BigDecimal.ZERO;

    @Column(name = "expiring_soon_points", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal expiringSoonPoints = BigDecimal.ZERO;

    @Column(name = "expiring_soon_date")
    private LocalDateTime expiringSoonDate;

    @Column(name = "tier", length = 20, nullable = false)
    @Builder.Default
    private String tier = "BRONZE"; // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND

    @Column(name = "tier_updated_at")
    private LocalDateTime tierUpdatedAt;

    // ===== Business Methods =====

    /**
     * 포인트 적립
     */
    public void earnPoints(BigDecimal points) {
        this.availablePoints = this.availablePoints.add(points);
        this.totalEarned = this.totalEarned.add(points);
    }

    /**
     * 포인트 사용
     */
    public void spendPoints(BigDecimal points) {
        if (this.availablePoints.compareTo(points) < 0) {
            throw new IllegalStateException("포인트가 부족합니다");
        }
        this.availablePoints = this.availablePoints.subtract(points);
        this.totalSpent = this.totalSpent.add(points);
    }

    /**
     * 포인트 만료
     */
    public void expirePoints(BigDecimal points) {
        this.availablePoints = this.availablePoints.subtract(points);
        this.totalExpired = this.totalExpired.add(points);
    }

    /**
     * 티어 업데이트
     */
    public void updateTier(String newTier) {
        this.tier = newTier;
        this.tierUpdatedAt = LocalDateTime.now();
    }

    /**
     * 곧 만료될 포인트 업데이트
     */
    public void updateExpiringSoon(BigDecimal points, LocalDateTime expiryDate) {
        this.expiringSoonPoints = points;
        this.expiringSoonDate = expiryDate;
    }
}
