package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 전체 요약 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {

    // 회원 통계
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisMonth;

    // 업체 통계
    private Long totalCompanies;
    private Long activeCompanies;

    // 견적요청 통계
    private Long totalEstimateRequests;
    private Long estimateRequestsInProgress;

    // 제안서 통계
    private Long totalProposals;
    private Double proposalSelectionRate;

    // 빠른상담 통계
    private Long pendingConsultations;

    // 플래너 신청 통계
    private Long pendingPlannerApplications;

    // 문의 통계
    private Long totalInquiries;
}
