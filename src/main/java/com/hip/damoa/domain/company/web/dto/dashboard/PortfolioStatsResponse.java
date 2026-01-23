package com.hip.damoa.domain.company.web.dto.dashboard;

import com.hip.damoa.domain.company.model.CompanyPortfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioStatsResponse {

    private long totalCount;    // 전체 포트폴리오 수
    private long totalViews;    // 전체 조회수 합계

    private List<TopPortfolio> topPortfolios;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPortfolio {
        private UUID uuid;
        private String title;
        private long viewCount;
        private String thumbnailUrl;

        public static TopPortfolio from(CompanyPortfolio portfolio) {
            return TopPortfolio.builder()
                    .uuid(portfolio.getUuid())
                    .title(portfolio.getTitle())
                    .viewCount(portfolio.getViewCount() != null ? portfolio.getViewCount() : 0)
                    .thumbnailUrl(portfolio.getThumbnailUrl())
                    .build();
        }
    }

    /**
     * 요약용 (전체 대시보드 요약에 포함)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalCount;
        private long totalViews;
    }
}
