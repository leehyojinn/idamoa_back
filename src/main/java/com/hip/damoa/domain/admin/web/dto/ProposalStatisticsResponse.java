package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 제안서 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (SUBMITTED, VIEWED, SELECTED, REJECTED, WITHDRAWN)
    private Map<String, Long> byStatus;

    // 선택률 (%)
    private Double selectionRate;

    // 평균 제안 금액
    private BigDecimal averagePrice;
}
