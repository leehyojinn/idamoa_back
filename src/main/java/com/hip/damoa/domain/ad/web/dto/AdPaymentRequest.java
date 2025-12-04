package com.hip.damoa.domain.ad.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 광고 추가 결제 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdPaymentRequest {

    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "500", message = "최소 결제 금액은 500원입니다")
    private BigDecimal paymentAmount;
}
