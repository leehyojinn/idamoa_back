package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기간 비교 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonStatsResponse {

    @Schema(
            description = "현재 기간 데이터\n" +
                    "요청한 현재 기간의 요약 통계입니다.\n" +
                    "활성 사용자, 세션, 페이지 뷰, 이탈률, 참여율, 평균 세션 시간이 포함됩니다."
    )
    private SummaryStatsData current;

    @Schema(
            description = "이전 기간 데이터\n" +
                    "비교 대상이 되는 이전 기간의 요약 통계입니다.\n" +
                    "현재 기간과 동일한 지표를 포함하며, 이를 통해 트렌드를 파악할 수 있습니다.\n" +
                    "예: 이번 주 vs 지난 주, 오늘 vs 어제 등의 비교가 가능합니다."
    )
    private SummaryStatsData previous;

    /**
     * 요약 통계 데이터 (내부 클래스)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryStatsData {

        @Schema(
                description = "활성 사용자 수\n" +
                        "해당 기간 동안의 고유 활성 사용자 수입니다.\n" +
                        "중복을 제거한 실제 방문자 수를 나타냅니다.",
                example = "1250"
        )
        private Long activeUsers;

        @Schema(
                description = "세션 수\n" +
                        "해당 기간 동안 발생한 총 세션 수입니다.\n" +
                        "세션은 사용자의 사이트 방문을 의미합니다.",
                example = "2000"
        )
        private Long sessions;

        @Schema(
                description = "페이지 뷰\n" +
                        "해당 기간 동안 조회된 총 페이지 수입니다.\n" +
                        "모든 페이지 조회를 합산한 값입니다.",
                example = "7500"
        )
        private Long pageViews;

        @Schema(
                description = "이탈률\n" +
                        "사용자가 한 페이지만 보고 떠난 비율입니다. (0.0 ~ 1.0)\n" +
                        "이탈률이 낮을수록 사용자가 여러 페이지를 탐색했다는 의미입니다.",
                example = "0.42"
        )
        private Double bounceRate;

        @Schema(
                description = "참여율\n" +
                        "사용자가 사이트에 참여한 비율입니다. (0.0 ~ 1.0)\n" +
                        "10초 이상 머물거나, 2개 이상의 페이지를 조회하거나, 전환 이벤트가 발생한 세션의 비율을 나타냅니다.",
                example = "0.68"
        )
        private Double engagementRate;

        @Schema(
                description = "평균 세션 시간 (초)\n" +
                        "사용자가 사이트에서 평균적으로 머문 시간입니다.\n" +
                        "세션당 평균 체류 시간을 초 단위로 나타냅니다.",
                example = "180.5"
        )
        private Double averageSessionDuration;
    }
}
