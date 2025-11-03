package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPurchaseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum purchase amount is 1000 KRW")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;  // CARD, KAKAO_PAY, TOSS, etc.
}
