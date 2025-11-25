package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 문의 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
    private Map<String, Long> byStatus;

    // 문의 유형별 통계 (PARTNERSHIP, ADVERTISING, 기타)
    private Map<String, Long> byType;
}
