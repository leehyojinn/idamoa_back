package com.hip.damoa.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Cancel Response DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelResponse {
    private String pgTransactionId;   // PG사 거래 ID
    private String orderId;           // 주문 ID
    private BigDecimal canceledAmount; // 취소 금액
    private String cancelReason;      // 취소 사유
    private LocalDateTime canceledAt; // 취소 시각
}
