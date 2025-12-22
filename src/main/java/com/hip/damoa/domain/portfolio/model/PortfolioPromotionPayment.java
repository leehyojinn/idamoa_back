package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 포트폴리오 프로모션 결제 내역
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_promotion_payments", indexes = {
    @Index(name = "idx_portfolio_promotion_payments_promotion_id", columnList = "promotion_id"),
    @Index(name = "idx_portfolio_promotion_payments_payment_date", columnList = "payment_date")
})
public class PortfolioPromotionPayment extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PortfolioPromotion promotion;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "apply_from_date", nullable = false)
    private LocalDate applyFromDate;

    @Column(name = "apply_to_date", nullable = false)
    private LocalDate applyToDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PortfolioPromotionPaymentType paymentType;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "credit_transaction_id")
    private Long creditTransactionId;

    // ===== Business Methods =====

    /**
     * 결제 성공 처리
     */
    public void complete() {
        this.status = "COMPLETED";
    }

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
}
