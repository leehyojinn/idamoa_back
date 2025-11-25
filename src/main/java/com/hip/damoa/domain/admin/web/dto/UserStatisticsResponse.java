package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 회원 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (ACTIVE, PENDING, INACTIVE, SUSPENDED)
    private Map<String, Long> byStatus;

    // 역할별 통계 (USER, COMPANY, ADMIN)
    private Map<String, Long> byRole;

    // 인증 현황
    private VerificationStatistics verification;

    // 마케팅 동의
    private Long marketingAgreed;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationStatistics {
        private Long emailVerified;
        private Long phoneVerified;
        private Long identityVerified;
    }
}
