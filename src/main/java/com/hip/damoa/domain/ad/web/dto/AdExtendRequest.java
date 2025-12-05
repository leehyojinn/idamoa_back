package com.hip.damoa.domain.ad.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 광고 캠페인 연장 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdExtendRequest {

    @NotNull(message = "연장 기간은 필수입니다")
    @Min(value = 7, message = "최소 연장 기간은 7일입니다")
    @Max(value = 30, message = "최대 연장 기간은 30일입니다")
    private Integer extensionDays;  // 7, 14, 30일

    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "3500", message = "최소 결제 금액은 3,500원입니다")
    private BigDecimal paymentAmount;
}
