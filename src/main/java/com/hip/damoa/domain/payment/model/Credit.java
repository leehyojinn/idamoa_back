package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자 크레딧/포인트
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credits", indexes = {
    @Index(name = "idx_credits_user_id", columnList = "user_id", unique = true)
})
public class Credit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Column(name = "available_credits", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal availableCredits = BigDecimal.ZERO;

    @Column(name = "expiring_credits", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal expiringCredits = BigDecimal.ZERO;

    @Column(name = "expiring_at")
    private LocalDateTime expiringAt;

    @Column(name = "total_earned", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_spent", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    public void earn(BigDecimal amount) {
        this.availableCredits = this.availableCredits.add(amount);
        this.totalEarned = this.totalEarned.add(amount);
    }

    public void spend(BigDecimal amount) {
        if (this.availableCredits.compareTo(amount) < 0) {
            throw new IllegalStateException("크레딧이 부족합니다");
        }
        this.availableCredits = this.availableCredits.subtract(amount);
        this.totalSpent = this.totalSpent.add(amount);
    }

    public void expire(BigDecimal amount) {
        this.availableCredits = this.availableCredits.subtract(amount);
    }
}
