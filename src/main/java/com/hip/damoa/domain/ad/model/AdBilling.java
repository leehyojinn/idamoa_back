package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 광고 청구 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_billings", indexes = {
    @Index(name = "idx_ad_billings_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_billings_billing_date", columnList = "billing_date"),
    @Index(name = "idx_ad_billings_status", columnList = "status")
})
public class AdBilling extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "billing_type", nullable = false, length = 20)
    private String billingType; // CPM, CPC, CPA, FLAT

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PAID, OVERDUE, CANCELLED

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ===== Business Methods =====

    /**
     * 결제 완료 처리
     */
    public void markAsPaid(String paymentMethod) {
        this.status = "PAID";
        this.paidAt = java.time.LocalDateTime.now();
        this.paymentMethod = paymentMethod;
    }

    /**
     * 청구 취소
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * 연체 처리
     */
    public void markAsOverdue() {
        this.status = "OVERDUE";
    }

    /**
     * 총액 계산
     */
    public void calculateTotal() {
        this.totalAmount = this.amount.add(this.taxAmount);
    }
}
