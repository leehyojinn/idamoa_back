package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.Refund;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 크레딧 환불 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRefundResponse {

    private UUID refundUuid;
    private BigDecimal requestedAmount;     // 요청 환불 금액
    private BigDecimal feeAmount;           // 수수료
    private BigDecimal actualRefundAmount;  // 실제 환불 금액
    private String status;                  // PENDING, COMPLETED, REJECTED
    private String refundReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static CreditRefundResponse from(Refund refund, BigDecimal feeAmount) {
        BigDecimal actualAmount = refund.getRefundAmount();
        BigDecimal requestedAmount = actualAmount.add(feeAmount);

        return CreditRefundResponse.builder()
                .refundUuid(refund.getUuid())
                .requestedAmount(requestedAmount)
                .feeAmount(feeAmount)
                .actualRefundAmount(actualAmount)
                .status(refund.getStatus())
                .refundReason(refund.getRefundReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }
}
