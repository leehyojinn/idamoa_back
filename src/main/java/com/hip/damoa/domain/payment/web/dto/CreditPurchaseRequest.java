package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 크레딧 충전 요청 DTO
 *
 * 패키지 코드: KRW_10000, KRW_30000, KRW_50000, KRW_100000
 * 수량: 사용자가 직접 지정 (1 이상)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPurchaseRequest {

    /**
     * 패키지 코드 (KRW_10000, KRW_30000, KRW_50000, KRW_100000)
     */
    @NotBlank(message = "패키지 코드는 필수입니다")
    private String packageCode;

    /**
     * 수량 (1 이상)
     */
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private Integer quantity;

    /**
     * 결제 수단 (KAKAOPAY, TOSS 등)
     */
    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;

    // 결제 콜백 URL (프론트엔드 리다이렉트용)
    private String successUrl;
    private String failUrl;
}
