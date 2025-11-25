package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여도 지표 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementMetricsResponse {

    @Schema(
            description = "이탈률\n" +
                    "사용자가 한 페이지만 보고 떠난 비율입니다. (0.0 ~ 1.0)\n" +
                    "이탈률이 낮을수록 사용자가 여러 페이지를 탐색하며 사이트에 관심을 보인다는 의미입니다.",
            example = "0.42"
    )
    private Double bounceRate;

    @Schema(
            description = "참여율\n" +
                    "사용자가 사이트에 참여한 비율입니다. (0.0 ~ 1.0)\n" +
                    "10초 이상 머물거나, 2개 이상의 페이지를 조회하거나, 전환 이벤트가 발생한 세션의 비율을 나타냅니다.\n" +
                    "참여율이 높을수록 사용자가 사이트에 적극적으로 참여한다는 의미입니다.",
            example = "0.68"
    )
    private Double engagementRate;

    @Schema(
            description = "평균 세션 시간 (초)\n" +
                    "사용자가 사이트에서 평균적으로 머문 시간입니다.\n" +
                    "세션당 평균 체류 시간을 초 단위로 나타냅니다.\n" +
                    "예: 180.5는 약 3분 동안 머물렀다는 의미입니다.",
            example = "180.5"
    )
    private Double averageSessionDuration;

    @Schema(
            description = "세션당 페이지 뷰\n" +
                    "한 세션 동안 평균적으로 조회한 페이지 수입니다.\n" +
                    "값이 높을수록 사용자가 여러 페이지를 탐색한다는 의미입니다.\n" +
                    "예: 3.5는 한 번 방문할 때 평균 3.5개의 페이지를 본다는 의미입니다.",
            example = "3.5"
    )
    private Double pageViewsPerSession;
}
