package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 크레딧 패키지 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPackageUpdateRequest {

    /**
     * 표시 이름 (선택)
     */
    private String displayName;

    /**
     * 보너스율 (%) - 3만원 이상만 적용
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
