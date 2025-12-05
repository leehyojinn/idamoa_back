package com.hip.damoa.domain.ad.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 광고 캠페인 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaignCreateRequest {

    @NotNull(message = "회사 UUID는 필수입니다")
    private UUID companyUuid;

    @Size(max = 200, message = "캠페인 이름은 200자를 초과할 수 없습니다")
    private String name;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "광고 기간은 필수입니다")
    @Min(value = 7, message = "최소 광고 기간은 7일입니다")
    @Max(value = 30, message = "최대 광고 기간은 30일입니다")
    private Integer durationDays;  // 7, 14, 30일

    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "3500", message = "최소 결제 금액은 3,500원입니다 (7일 x 500원)")
    private BigDecimal paymentAmount;

    private Boolean autoRenew;
}
