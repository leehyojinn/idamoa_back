package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 빠른상담 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (SUBMITTED, IN_PROGRESS, COMPLETED, CANCELLED)
    private Map<String, Long> byStatus;

    // 회원/비회원 비율
    private MemberVsNonMember memberVsNonMember;

    // 업체 배정 수
    private Long assigned;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberVsNonMember {
        private Long member;
        private Long nonMember;
    }
}
