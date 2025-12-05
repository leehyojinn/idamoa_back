package com.hip.damoa.domain.payment.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 관리자용 크레딧 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreditStatsResponse {

    // 전체 통계
    private long totalUsers;                    // 크레딧 보유 사용자 수
    private BigDecimal totalAvailableCredits;   // 전체 가용 크레딧
    private BigDecimal totalEarnedCredits;      // 전체 적립 크레딧
    private BigDecimal totalSpentCredits;       // 전체 사용 크레딧

    // 결제 통계
    private long totalPaymentCount;             // 총 결제 건수
    private BigDecimal totalPaymentAmount;      // 총 결제 금액

    // 환불 통계
    private long pendingRefundCount;            // 대기 중 환불 건수
    private long completedRefundCount;          // 완료 환불 건수
    private long rejectedRefundCount;           // 거부 환불 건수
    private BigDecimal totalRefundedAmount;     // 총 환불 금액
    private BigDecimal pendingRefundAmount;     // 대기 중 환불 금액

    // 패키지별 통계
    private long activePackageCount;            // 활성 패키지 수
}
