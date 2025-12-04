package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 광고 결제 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdPaymentResponse {

    private UUID uuid;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private LocalDate applyFromDate;
    private LocalDate applyToDate;
    private Integer applyDays;

    /**
     * 1일 가치 (결제금액 / 적용일수)
     * 예: 7일에 700원 → dailyValue = 100원
     */
    private BigDecimal dailyValue;

    private String paymentType;
    private String status;
    private Integer remainingDays;
    private LocalDateTime createdAt;

    public static AdPaymentResponse from(AdPayment payment) {
        return AdPaymentResponse.builder()
                .uuid(payment.getUuid())
                .paymentAmount(payment.getPaymentAmount())
                .paymentDate(payment.getPaymentDate())
                .applyFromDate(payment.getApplyFromDate())
                .applyToDate(payment.getApplyToDate())
                .applyDays(payment.getApplyDays())
                .dailyValue(payment.getDailyValue())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .remainingDays(payment.getRemainingDays())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
