package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 관리자 크레딧 수동 조정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreditAdjustRequest {

    /**
     * 조정 금액 (양수: 지급, 음수: 차감)
     */
    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다")
    private BigDecimal amount;

    /**
     * 조정 사유
     */
    @NotBlank(message = "사유는 필수입니다")
    private String reason;
}
