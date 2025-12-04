package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 크레딧 패키지 생성 요청 DTO
 *
 * 패키지는 단위 금액(10000, 30000, 50000, 100000)으로 구분됩니다.
 * 수량은 사용자가 충전 시 직접 지정합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPackageCreateRequest {

    /**
     * 단위 금액 (10000, 30000, 50000, 100000)
     */
    @NotNull(message = "단위 금액은 필수입니다")
    private Integer unitAmount;

    /**
     * 보너스율 (%) - 3만원 이상만 적용
     * 예: 5.00 = 5%
     */
    @DecimalMin(value = "0", message = "보너스율은 0 이상이어야 합니다")
    @DecimalMax(value = "100", message = "보너스율은 100 이하여야 합니다")
    private BigDecimal bonusRate;

    /**
     * 최대 보너스 한도 (null이면 무제한)
     */
    @Min(value = 0, message = "최대 보너스는 0 이상이어야 합니다")
    private Integer maxBonus;

    /**
     * 설명
     */
    private String description;
}
