package com.hip.damoa.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Query Response DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentQueryResponse {
    private String pgTransactionId;   // PG사 거래 ID
    private String orderId;           // 주문 ID
    private String status;            // 결제 상태 (READY, APPROVED, CANCELLED, FAILED)
    private BigDecimal amount;        // 결제 금액
    private BigDecimal canceledAmount; // 취소된 금액
    private LocalDateTime approvedAt;  // 승인 시각
    private LocalDateTime canceledAt;  // 취소 시각 (선택)
}
