package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 크레딧 충전 패키지 (단위 금액 기준)
 *
 * 패키지 유형 (4개):
 * - 1만원권: 보너스 없음
 * - 3만원권: 보너스 적용
 * - 5만원권: 보너스 적용
 * - 10만원권: 보너스 적용
 *
 * 수량은 사용자가 충전 시 직접 지정 (무제한)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credit_packages", indexes = {
    @Index(name = "idx_credit_packages_unit_amount", columnList = "unit_amount"),
    @Index(name = "idx_credit_packages_is_active", columnList = "is_active"),
    @Index(name = "idx_credit_packages_display_order", columnList = "display_order")
})
public class CreditPackage extends BaseEntity {

    /**
     * 패키지 코드 (예: KRW_10000, KRW_30000)
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 표시 이름 (예: "1만원권", "3만원권")
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * 단위 금액 (10000, 30000, 50000, 100000)
     */
    @Column(name = "unit_amount", nullable = false)
    private Integer unitAmount;

    /**
     * 보너스율 (%) - 3만원 이상부터 적용
     * 예: 5 = 5%
     */
    @Column(name = "bonus_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal bonusRate = BigDecimal.ZERO;

    /**
     * 최대 보너스 한도 (null이면 무제한)
     */
    @Column(name = "max_bonus")
    private Integer maxBonus;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 표시 순서
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    // ===== Static Factory Methods =====

    /**
     * 새 패키지 생성
     */
    public static CreditPackage create(Integer unitAmount, BigDecimal bonusRate, Integer maxBonus) {
        // 3만원 미만은 보너스 없음
        if (unitAmount < 30000) {
            bonusRate = BigDecimal.ZERO;
            maxBonus = null;
        }

        String code = "KRW_" + unitAmount;
        String displayName = formatDisplayName(unitAmount);

        return CreditPackage.builder()
                .code(code)
                .displayName(displayName)
                .unitAmount(unitAmount)
                .bonusRate(bonusRate != null ? bonusRate : BigDecimal.ZERO)
                .maxBonus(maxBonus)
                .isActive(true)
                .displayOrder(unitAmount / 10000)
                .build();
    }

    // ===== Business Methods =====

    /**
     * 보너스율 변경
     */
    public void updateBonusRate(BigDecimal newBonusRate, Integer newMaxBonus) {
        // 3만원 미만은 보너스 적용 불가
        if (this.unitAmount < 30000) {
            this.bonusRate = BigDecimal.ZERO;
            this.maxBonus = null;
            return;
        }
        this.bonusRate = newBonusRate != null ? newBonusRate : BigDecimal.ZERO;
        this.maxBonus = newMaxBonus;
    }

    /**
     * 패키지 정보 업데이트
     */
    public void update(String displayName, BigDecimal bonusRate, Integer maxBonus, String description) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        updateBonusRate(bonusRate, maxBonus);
        this.description = description;
    }

    /**
     * 특정 수량에 대한 결제 금액 계산
     */
    public int calculatePaymentAmount(int quantity) {
        return this.unitAmount * quantity;
    }

    /**
     * 특정 수량에 대한 보너스 크레딧 계산
     */
    public int calculateBonusCredits(int quantity) {
        if (this.unitAmount < 30000 || this.bonusRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        int baseCredits = this.unitAmount * quantity;
        int bonusCredits = BigDecimal.valueOf(baseCredits)
                .multiply(this.bonusRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();

        // 최대 보너스 한도 적용
        if (this.maxBonus != null && bonusCredits > this.maxBonus) {
            bonusCredits = this.maxBonus;
        }

        return bonusCredits;
    }

    /**
     * 특정 수량에 대한 총 크레딧 계산
     */
    public int calculateTotalCredits(int quantity) {
        int baseCredits = this.unitAmount * quantity;
        int bonusCredits = calculateBonusCredits(quantity);
        return baseCredits + bonusCredits;
    }

    /**
     * 활성화/비활성화
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * 표시 순서 변경
     */
    public void setDisplayOrder(int order) {
        this.displayOrder = order;
    }

    // ===== Helper Methods =====

    private static String formatDisplayName(int unitAmount) {
        if (unitAmount >= 10000) {
            return (unitAmount / 10000) + "만원권";
        } else {
            return unitAmount + "원권";
        }
    }

    /**
     * 보너스 적용 대상인지 확인 (3만원 이상)
     */
    public boolean isBonusEligible() {
        return this.unitAmount >= 30000;
    }
}
