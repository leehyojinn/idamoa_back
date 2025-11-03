package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 내역
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_payment_id", columnList = "payment_id"),
    @Index(name = "idx_refunds_status", columnList = "status")
})
public class Refund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "TEXT", nullable = false)
    private String refundReason;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED, REJECTED

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public void complete() {
        this.status = "COMPLETED";
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = "REJECTED";
        this.rejectionReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = "FAILED";
        this.processedAt = LocalDateTime.now();
    }
}
