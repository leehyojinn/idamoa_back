package com.hip.damoa.domain.company.web.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체 대시보드 전체 요약 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDashboardSummaryResponse {

    private ConsultationStatsResponse.Summary consultation;
    private ReviewStatsResponse.Summary review;
    private EstimateStatsResponse.Summary estimate;
    private ChatStatsResponse.Summary chat;
    private CompanyStats company;
    private PortfolioStatsResponse.Summary portfolio;

    /**
     * 업체 기본 통계 (좋아요, 조회수)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyStats {
        private Integer likeCount;
        private Integer viewCount;
    }
}
