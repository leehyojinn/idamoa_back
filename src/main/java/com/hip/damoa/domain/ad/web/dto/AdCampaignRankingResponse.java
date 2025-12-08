package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdCampaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 광고 캠페인 순위 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "광고 캠페인 순위 응답")
public class AdCampaignRankingResponse {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "캠페인 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID campaignUuid;

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    @Schema(description = "우선순위 점수 (= totalDailyValue)", example = "1666.67")
    private BigDecimal priorityScore;

    @Schema(description = "1일 가치 (모든 활성 결제의 dailyValue 합, 원/일)", example = "1666.67")
    private BigDecimal dailyValue;

    @Schema(description = "종료일", example = "2025-01-31")
    private LocalDate endDate;

    @Schema(description = "남은 일수", example = "15")
    private Integer remainingDays;

    public static AdCampaignRankingResponse from(AdCampaign campaign, int rank) {
        return AdCampaignRankingResponse.builder()
                .rank(rank)
                .campaignUuid(campaign.getUuid())
                .companyUuid(campaign.getCompany().getUuid())
                .companyName(campaign.getCompany().getName())
                .priorityScore(campaign.getPriorityScore())
                .dailyValue(campaign.getTotalDailyValue())
                .endDate(campaign.getEndDate())
                .remainingDays(campaign.getRemainingDays())
                .build();
    }
}
