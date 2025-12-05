package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 거부 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundRejectRequest {

    /**
     * 거부 사유
     */
    @NotBlank(message = "거부 사유는 필수입니다")
    private String rejectionReason;
}
