package com.hip.damoa.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Approval Response DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentApprovalResponse {
    private String pgTransactionId;   // PG사 거래 ID
    private String orderId;           // 주문 ID
    private String paymentMethod;     // 결제 수단 (카드, 계좌이체 등)
    private BigDecimal amount;        // 결제 금액
    private LocalDateTime approvedAt; // 승인 시각
    private String receiptUrl;        // 영수증 URL (선택)
}
