package com.hip.damoa.domain.analytics.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요약 통계 Response (메인 대시보드용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryStatsResponse {

    private Long activeUsers;       // 활성 사용자 수
    private Long sessions;           // 세션 수
    private Long screenPageViews;    // 페이지 뷰 수
    private Double bounceRate;       // 이탈률 (%)
    private Double engagementRate;   // 참여율 (%)
    private Double avgSessionDuration; // 평균 세션 시간 (초)
}
