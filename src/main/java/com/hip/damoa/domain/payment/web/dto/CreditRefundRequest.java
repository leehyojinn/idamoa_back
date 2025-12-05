package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 크레딧 환불 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRefundRequest {

    @NotNull(message = "환불 금액은 필수입니다")
    @DecimalMin(value = "1000.00", message = "최소 환불 금액은 1,000원입니다")
    private BigDecimal refundAmount;  // 환불 요청 크레딧 금액

    @NotBlank(message = "환불 사유는 필수입니다")
    @Size(max = 500, message = "환불 사유는 500자를 초과할 수 없습니다")
    private String refundReason;

    // 환불 계좌 정보
    @NotBlank(message = "은행명은 필수입니다")
    private String bankName;

    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^[0-9-]+$", message = "올바른 계좌번호 형식이 아닙니다")
    private String accountNumber;

    @NotBlank(message = "예금주명은 필수입니다")
    private String accountHolder;
}
