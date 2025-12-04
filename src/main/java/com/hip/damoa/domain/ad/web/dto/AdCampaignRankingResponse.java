package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdCampaign;
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
public class AdCampaignRankingResponse {

    private int rank;
    private UUID campaignUuid;
    private UUID companyUuid;
    private String companyName;

    /**
     * 우선순위 점수 (= totalDailyValue)
     */
    private BigDecimal priorityScore;

    /**
     * 1일 가치 (모든 활성 결제의 dailyValue 합)
     */
    private BigDecimal dailyValue;

    private LocalDate endDate;
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
