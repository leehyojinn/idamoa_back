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
 * 관리자용 거래 내역 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionResponse {

    private UUID transactionUuid;
    private UUID userUuid;
    private String userEmail;
    private String userName;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String reason;
    private String entityType;
    private Long entityId;
    private LocalDateTime createdAt;

    public static AdminTransactionResponse from(CreditTransaction transaction) {
        return AdminTransactionResponse.builder()
                .transactionUuid(transaction.getUuid())
                .userUuid(transaction.getUser().getUuid())
                .userEmail(transaction.getUser().getEmail())
                .userName(transaction.getUser().getEmail())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .reason(transaction.getReason())
                .entityType(transaction.getEntityType())
                .entityId(transaction.getEntityId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
