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

    // 프론트엔드 리다이렉트 URL (선택 - 제공되지 않으면 서버 설정 사용)
    private String successUrl;        // 결제 성공 시 리다이렉트 URL
    private String failUrl;           // 결제 실패 시 리다이렉트 URL
    private String cancelUrl;         // 결제 취소 시 리다이렉트 URL (카카오페이용)
}
