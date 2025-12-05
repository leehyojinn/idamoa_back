package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.AdminCreditStatsService;
import com.hip.damoa.domain.payment.web.dto.AdminCreditStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 크레딧 통계 대시보드 컨트롤러
 */
@Slf4j
@Tag(name = "1925. Admin Credit Stats", description = "관리자 크레딧 통계 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/credit-stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCreditStatsController {

    private final AdminCreditStatsService adminCreditStatsService;

    @Operation(summary = "종합 크레딧 통계 대시보드",
            description = """
            크레딧 시스템의 종합 통계를 조회합니다.

            **크레딧 통계**
            - totalUsers: 크레딧 보유 사용자 수
            - totalAvailableCredits: 전체 가용 크레딧 합계
            - totalEarnedCredits: 전체 적립 크레딧 합계
            - totalSpentCredits: 전체 사용 크레딧 합계

            **결제 통계**
            - totalPaymentCount: 완료된 결제 건수
            - totalPaymentAmount: 완료된 결제 총액

            **환불 통계**
            - pendingRefundCount: 대기 중인 환불 건수
            - completedRefundCount: 완료된 환불 건수
            - rejectedRefundCount: 거부된 환불 건수
            - totalRefundedAmount: 환불 완료 총액
            - pendingRefundAmount: 대기 중인 환불 금액

            **패키지 통계**
            - activePackageCount: 활성 패키지 수
            """)
    @GetMapping
    public ApiResponse<AdminCreditStatsResponse> getCreditStats() {
        log.info("종합 크레딧 통계 조회");
        AdminCreditStatsResponse stats = adminCreditStatsService.getCreditStats();
        return ApiResponse.success(stats);
    }

    @Operation(summary = "크레딧 잔액 통계",
            description = """
            크레딧 잔액 관련 통계만 조회합니다.

            **반환 정보**
            - totalAvailableCredits: 전체 가용 크레딧 합계
            - totalEarnedCredits: 전체 적립 크레딧 합계
            - totalSpentCredits: 전체 사용 크레딧 합계
            - usersWithCredits: 크레딧 보유 사용자 수
            """)
    @GetMapping("/balance")
    public ApiResponse<AdminCreditStatsService.CreditBalanceStats> getCreditBalanceStats() {
        log.info("크레딧 잔액 통계 조회");
        AdminCreditStatsService.CreditBalanceStats stats = adminCreditStatsService.getCreditBalanceStats();
        return ApiResponse.success(stats);
    }
}
