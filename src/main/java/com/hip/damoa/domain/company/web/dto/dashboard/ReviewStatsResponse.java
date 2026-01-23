package com.hip.damoa.domain.company.web.dto.dashboard;

import com.hip.damoa.domain.company.model.CompanyReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 리뷰 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    private long totalCount;
    private Double averageScore;
    private Map<Integer, Long> scoreDistribution;  // 점수별 개수 (1~5)

    private List<RecentReview> recentReviews;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReview {
        private UUID uuid;
        private String nickname;
        private BigDecimal score;
        private String content;
        private LocalDateTime createdAt;

        public static RecentReview from(CompanyReview review) {
            return RecentReview.builder()
                    .uuid(review.getUuid())
                    .nickname(review.getUser() != null ? review.getUser().getEmail() : "익명")
                    .score(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
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
        private Double averageScore;
        private int recentCount;  // 최근 7일간 리뷰 수
    }
}
