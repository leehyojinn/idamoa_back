package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.CreditTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 크레딧 거래 내역 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransactionResponse {

    private UUID uuid;
    private String transactionType;     // EARN, SPEND, EXPIRE, REFUND
    private BigDecimal amount;          // 거래 금액 (음수: 차감, 양수: 적립)
    private BigDecimal balanceAfter;    // 거래 후 잔액
    private String reason;              // 거래 사유
    private String entityType;          // 관련 엔티티 타입
    private LocalDateTime createdAt;

    public static CreditTransactionResponse from(CreditTransaction tx) {
        return CreditTransactionResponse.builder()
                .uuid(tx.getUuid())
                .transactionType(tx.getTransactionType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .reason(tx.getReason())
                .entityType(tx.getEntityType())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
