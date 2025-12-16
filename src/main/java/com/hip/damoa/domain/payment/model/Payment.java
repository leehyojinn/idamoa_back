package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 결제 내역
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_user_id", columnList = "user_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Column(name = "payment_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "fee_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, KAKAOPAY, NAVERPAY

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED

    @Column(name = "entity_type", length = 50)
    private String entityType; // ESTIMATE, AD_CAMPAIGN, SUBSCRIPTION

    @Column(name = "entity_id")
    private Long entityId;

    @Type(JsonBinaryType.class)
    @Column(name = "payment_gateway_data", columnDefinition = "jsonb")
    private Map<String, Object> paymentGatewayData;

    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    /**
     * PG사 거래 ID (결제 승인 후 PG에서 반환)
     */
    @Column(name = "pg_transaction_id", length = 200)
    private String pgTransactionId;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // ===== Business Methods =====

    public void complete() {
        this.status = "COMPLETED";
        this.paidAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = "FAILED";
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void refund() {
        this.status = "REFUNDED";
    }

    /**
     * PG 거래 ID 설정 (결제 승인 후)
     */
    public void setPgTransactionId(String pgTransactionId) {
        this.pgTransactionId = pgTransactionId;
    }

    /**
     * 영수증 URL 설정
     */
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
}
