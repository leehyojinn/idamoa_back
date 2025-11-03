package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 광고 결제 이력 (V2 추가)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_payments", indexes = {
    @Index(name = "idx_ad_payments_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_payments_payment_date", columnList = "payment_date")
})
public class AdPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "payment_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "apply_days", nullable = false)
    private Integer applyDays;

    @Column(name = "daily_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal dailyRate;

    @Column(name = "value_30d", precision = 12, scale = 2, nullable = false)
    private BigDecimal value30d;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType; // INITIAL, ADDITIONAL, RENEWAL

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED"; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void calculateValues() {
        if (this.paymentAmount != null && this.applyDays != null && this.applyDays > 0) {
            // 일일 단가 = 결제금액 / 적용일수
            this.dailyRate = this.paymentAmount.divide(
                BigDecimal.valueOf(this.applyDays),
                2,
                RoundingMode.HALF_UP
            );

            // 30일 환산 가치 = 일일 단가 × 30
            this.value30d = this.dailyRate.multiply(BigDecimal.valueOf(30));
        }
    }

    // ===== Business Methods =====

    /**
     * 환불 처리
     */
    public void refund() {
        this.status = "REFUNDED";
    }

    /**
     * 결제 실패 처리
     */
    public void fail() {
        this.status = "FAILED";
    }

    public enum PaymentType {
        INITIAL,    // 최초 결제
        ADDITIONAL, // 추가 결제
        RENEWAL     // 갱신 결제
    }
}
