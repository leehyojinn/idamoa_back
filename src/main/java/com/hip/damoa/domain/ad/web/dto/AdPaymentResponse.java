package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdPayment;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "광고 결제 응답")
public class AdPaymentResponse {

    @Schema(description = "결제 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "결제 금액 (원)", example = "7000")
    private BigDecimal paymentAmount;

    @Schema(description = "결제일", example = "2025-01-01")
    private LocalDate paymentDate;

    @Schema(description = "적용 시작일", example = "2025-01-01")
    private LocalDate applyFromDate;

    @Schema(description = "적용 종료일", example = "2025-01-07")
    private LocalDate applyToDate;

    @Schema(description = "적용 일수", example = "7")
    private Integer applyDays;

    @Schema(description = "1일 가치 (결제금액 / 적용일수)", example = "1000")
    private BigDecimal dailyValue;

    @Schema(description = "결제 유형 (INITIAL, ADDITIONAL)", example = "INITIAL")
    private String paymentType;

    @Schema(description = "결제 상태 (ACTIVE, CONSUMED, REFUNDED)", example = "ACTIVE")
    private String status;

    @Schema(description = "남은 일수", example = "5")
    private Integer remainingDays;

    @Schema(description = "생성일시", example = "2025-01-01T09:00:00")
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
