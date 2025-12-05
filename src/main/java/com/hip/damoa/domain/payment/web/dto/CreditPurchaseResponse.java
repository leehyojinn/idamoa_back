package com.hip.damoa.domain.payment.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 크레딧 충전 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPurchaseResponse {

    private UUID paymentUuid;           // 결제 UUID
    private String orderId;             // 주문 ID
    private String paymentUrl;          // 결제 페이지 URL (카카오페이 등)
    private BigDecimal paymentAmount;   // 결제 금액
    private BigDecimal totalCredits;    // 총 충전 예정 크레딧
    private BigDecimal bonusCredits;    // 보너스 크레딧
    private String packageDisplayName;  // 패키지 표시명
}
