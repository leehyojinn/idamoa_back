package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 견적요청 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED)
    private Map<String, Long> byStatus;

    // 공개 요청 수
    private Long publicRequests;

    // 평균 제안서 수 (견적요청당)
    private Double averageProposalsPerRequest;

    // 총 조회 수
    private Long totalViews;
}
