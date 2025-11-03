package com.hip.damoa.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment Prepare Response DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareResponse {
    private String paymentUrl;        // 결제 페이지 URL
    private String pgTransactionId;   // PG사 거래 ID (TID)
    private String orderId;           // 주문 ID
}
