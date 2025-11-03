package com.hip.damoa.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment Prepare Request DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequest {
    private String orderId;           // 주문 ID (unique)
    private String orderName;         // 주문명
    private BigDecimal amount;        // 결제 금액
    private String customerEmail;     // 고객 이메일
    private String customerName;      // 고객 이름
    private String customerPhone;     // 고객 전화번호 (선택)
}
