package com.hip.damoa.domain.payment.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 크레딧 거래 내역
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credit_transactions", indexes = {
    @Index(name = "idx_credit_transactions_user_id", columnList = "user_id"),
    @Index(name = "idx_credit_transactions_type", columnList = "transaction_type"),
    @Index(name = "idx_credit_transactions_created_at", columnList = "created_at")
})
public class CreditTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id", nullable = false)
    private Credit credit;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // EARN, SPEND, EXPIRE, REFUND, ADMIN_GRANT, ADMIN_DEDUCT

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 12, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;
}
