package com.hip.damoa.domain.payment.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 크레딧 잔액 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "크레딧 잔액 응답")
public class CreditBalanceResponse {

    @Schema(description = "크레딧 잔액", example = "50000")
    private BigDecimal balance;

    @Schema(description = "통화 단위", example = "KRW")
    private String currency;
}
