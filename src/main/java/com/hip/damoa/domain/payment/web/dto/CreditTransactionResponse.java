package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.CreditTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "크레딧 거래 내역 응답")
public class CreditTransactionResponse {

    @Schema(description = "거래 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "거래 유형 (EARN: 적립, SPEND: 사용, EXPIRE: 만료, REFUND: 환불)", example = "EARN")
    private String transactionType;

    @Schema(description = "거래 금액 (양수: 적립, 음수: 차감)", example = "10000")
    private BigDecimal amount;

    @Schema(description = "거래 후 잔액", example = "50000")
    private BigDecimal balanceAfter;

    @Schema(description = "거래 사유", example = "크레딧 충전")
    private String reason;

    @Schema(description = "관련 엔티티 타입", example = "FILE_DOWNLOAD")
    private String entityType;

    @Schema(description = "거래일시", example = "2025-01-01T10:00:00")
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
