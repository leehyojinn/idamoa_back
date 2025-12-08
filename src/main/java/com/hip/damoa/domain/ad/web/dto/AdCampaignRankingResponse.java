package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdCampaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 광고 캠페인 순위 응답 DTO
 * - 공개 API용: 민감한 정보(priorityScore, dailyValue, endDate, remainingDays)는 노출하지 않음
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "광고 캠페인 순위 응답")
public class AdCampaignRankingResponse {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    public static AdCampaignRankingResponse from(AdCampaign campaign, int rank) {
        return AdCampaignRankingResponse.builder()
                .rank(rank)
                .companyUuid(campaign.getCompany().getUuid())
                .companyName(campaign.getCompany().getName())
                .build();
    }
}
