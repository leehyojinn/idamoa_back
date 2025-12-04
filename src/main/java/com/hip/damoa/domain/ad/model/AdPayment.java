package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 광고 캠페인 결제 내역
 *
 * 1일 가치(dailyValue) 계산:
 * - 7일에 700원 → dailyValue = 100원
 * - 추가 충전 600원 (남은 6일) → 추가 dailyValue = 100원
 * - 총 dailyValue = 200원
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_payments", indexes = {
    @Index(name = "idx_ad_payments_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_payments_status", columnList = "status"),
    @Index(name = "idx_ad_payments_apply_dates", columnList = "apply_from_date, apply_to_date")
})
public class AdPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "payment_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "apply_from_date", nullable = false)
    private LocalDate applyFromDate;

    @Column(name = "apply_to_date", nullable = false)
    private LocalDate applyToDate;

    @Column(name = "apply_days", nullable = false)
    private Integer applyDays;

    /**
     * 1일 가치 (결제금액 / 적용일수)
     * 이 값이 우선순위 계산의 기준
     */
    @Column(name = "daily_value", precision = 12, scale = 4)
    private BigDecimal dailyValue;

    @Column(name = "payment_type", nullable = false, length = 20)
    @Builder.Default
    private String paymentType = "INITIAL"; // INITIAL, ADDITIONAL

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CONSUMED, REFUNDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_transaction_id")
    private CreditTransaction creditTransaction;

    // ===== Static Factory Methods =====

    /**
     * 최초 결제 생성
     * 예: 7일에 700원 → dailyValue = 100원
     */
    public static AdPayment createInitial(AdCampaign campaign, BigDecimal amount,
                                           LocalDate startDate, LocalDate endDate,
                                           CreditTransaction creditTransaction) {
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        BigDecimal dailyValue = amount.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);

        return AdPayment.builder()
                .campaign(campaign)
                .paymentAmount(amount)
                .paymentDate(LocalDate.now())
                .applyFromDate(startDate)
                .applyToDate(endDate)
                .applyDays(days)
                .dailyValue(dailyValue)
                .paymentType("INITIAL")
                .status("ACTIVE")
                .creditTransaction(creditTransaction)
                .build();
    }

    /**
     * 추가 결제 생성
     * 예: 남은 6일에 600원 추가 → 추가 dailyValue = 100원
     */
    public static AdPayment createAdditional(AdCampaign campaign, BigDecimal amount,
                                              LocalDate applyFromDate, LocalDate applyToDate,
                                              CreditTransaction creditTransaction) {
        int days = (int) (applyToDate.toEpochDay() - applyFromDate.toEpochDay()) + 1;
        BigDecimal dailyValue = amount.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);

        return AdPayment.builder()
                .campaign(campaign)
                .paymentAmount(amount)
                .paymentDate(LocalDate.now())
                .applyFromDate(applyFromDate)
                .applyToDate(applyToDate)
                .applyDays(days)
                .dailyValue(dailyValue)
                .paymentType("ADDITIONAL")
                .status("ACTIVE")
                .creditTransaction(creditTransaction)
                .build();
    }

    // ===== Business Methods =====

    /**
     * 결제 소진 처리 (캠페인 종료 시)
     */
    public void consume() {
        this.status = "CONSUMED";
    }

    /**
     * 결제 환불 처리
     */
    public void refund() {
        this.status = "REFUNDED";
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * 현재 적용 기간 내인지 확인
     */
    public boolean isWithinPeriod() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(applyFromDate) && !today.isAfter(applyToDate);
    }

    /**
     * 남은 일수 계산
     */
    public int getRemainingDays() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(applyToDate)) {
            return 0;
        }
        if (today.isBefore(applyFromDate)) {
            return applyDays;
        }
        return (int) (applyToDate.toEpochDay() - today.toEpochDay()) + 1;
    }

    /**
     * 현재 기간 내 유효한 1일 가치 반환
     * - 기간 내: dailyValue 반환
     * - 기간 외: 0 반환
     */
    public BigDecimal getEffectiveDailyValue() {
        if (!isActive() || !isWithinPeriod()) {
            return BigDecimal.ZERO;
        }
        return dailyValue != null ? dailyValue : BigDecimal.ZERO;
    }
}
