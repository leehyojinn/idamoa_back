package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.AdminRefundService;
import com.hip.damoa.domain.payment.web.dto.AdminRefundRejectRequest;
import com.hip.damoa.domain.payment.web.dto.AdminRefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자용 환불 관리 컨트롤러
 */
@Slf4j
@Tag(name = "9923. Admin Refund", description = "관리자 환불 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/refunds")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    // ==================== 환불 목록 조회 ====================

    @Operation(summary = "환불 목록 조회",
            description = """
            모든 환불 내역을 페이징하여 조회합니다.

            **필터 옵션**
            - status: 환불 상태 (PENDING, COMPLETED, REJECTED)
            - keyword: 검색 (사용자 이메일/이름, 환불 사유)

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: amount, status
            """)
    @GetMapping
    public ApiResponse<Page<AdminRefundResponse>> getRefunds(
            @Parameter(description = "환불 상태 필터") @RequestParam(required = false) String status,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("환불 목록 조회: status={}, keyword={}", status, keyword);

        Page<AdminRefundResponse> refunds;
        if (keyword != null && !keyword.isBlank()) {
            refunds = adminRefundService.searchRefunds(keyword, pageable);
        } else if (status != null && !status.isBlank()) {
            refunds = adminRefundService.getRefundsByStatus(status, pageable);
        } else {
            refunds = adminRefundService.getAllRefunds(pageable);
        }

        return ApiResponse.success(refunds);
    }

    @Operation(summary = "대기 중인 환불 목록 조회",
            description = """
            처리가 필요한 대기 중인 환불 요청 목록을 조회합니다.

            ## 정렬
            - 기본값: createdAt ASC (오래된 순, 먼저 신청한 건부터)
            - 사용법: sort=createdAt,asc 또는 sort=createdAt,desc
            """)
    @GetMapping("/pending")
    public ApiResponse<Page<AdminRefundResponse>> getPendingRefunds(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("대기 중인 환불 목록 조회");
        Page<AdminRefundResponse> refunds = adminRefundService.getPendingRefunds(pageable);
        return ApiResponse.success(refunds);
    }

    @Operation(summary = "처리 완료된 환불 목록 조회",
            description = """
            처리 완료된 환불 목록을 조회합니다.

            ## 정렬
            - 기본값: updatedAt DESC (최근 처리된 건부터)
            - 사용법: sort=updatedAt,desc 또는 sort=updatedAt,asc
            """)
    @GetMapping("/completed")
    public ApiResponse<Page<AdminRefundResponse>> getCompletedRefunds(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("처리 완료된 환불 목록 조회");
        Page<AdminRefundResponse> refunds = adminRefundService.getCompletedRefunds(pageable);
        return ApiResponse.success(refunds);
    }

    @Operation(summary = "환불 상세 조회",
            description = "특정 환불 요청의 상세 정보를 조회합니다.")
    @GetMapping("/{refundUuid}")
    public ApiResponse<AdminRefundResponse> getRefund(
            @Parameter(description = "환불 UUID") @PathVariable UUID refundUuid) {

        log.info("환불 상세 조회: uuid={}", refundUuid);
        AdminRefundResponse refund = adminRefundService.getRefund(refundUuid);
        return ApiResponse.success(refund);
    }

    // ==================== 환불 처리 ====================

    @Operation(summary = "환불 승인",
            description = """
            환불 요청을 승인합니다.

            **처리 내용**
            - 환불 상태를 COMPLETED로 변경
            - 실제 계좌 송금은 별도로 진행해야 합니다

            **주의사항**
            - 이미 처리된 환불은 다시 승인할 수 없습니다
            """)
    @PostMapping("/{refundUuid}/approve")
    public ApiResponse<AdminRefundResponse> approveRefund(
            @Parameter(description = "환불 UUID") @PathVariable UUID refundUuid) {

        log.info("환불 승인: uuid={}", refundUuid);
        AdminRefundResponse refund = adminRefundService.approveRefund(refundUuid);
        return ApiResponse.success(refund);
    }

    @Operation(summary = "환불 거부",
            description = """
            환불 요청을 거부합니다.

            **처리 내용**
            - 환불 상태를 REJECTED로 변경
            - 사용자의 크레딧을 원래대로 복구 (수수료 포함 금액)
            - 거래 내역에 복구 기록 추가

            **필수 정보**
            - reason: 거부 사유 (사용자에게 표시됨)

            **주의사항**
            - 이미 처리된 환불은 다시 거부할 수 없습니다
            """)
    @PostMapping("/{refundUuid}/reject")
    public ApiResponse<AdminRefundResponse> rejectRefund(
            @Parameter(description = "환불 UUID") @PathVariable UUID refundUuid,
            @Valid @RequestBody AdminRefundRejectRequest request) {

        log.info("환불 거부: uuid={}, reason={}", refundUuid, request.getRejectionReason());
        AdminRefundResponse refund = adminRefundService.rejectRefund(refundUuid, request);
        return ApiResponse.success(refund);
    }

    // ==================== 통계 ====================

    @Operation(summary = "환불 통계",
            description = """
            환불 관련 통계 정보를 조회합니다.

            **반환 정보**
            - pendingCount: 대기 중인 환불 건수
            - completedCount: 완료된 환불 건수
            - rejectedCount: 거부된 환불 건수
            - totalRefundedAmount: 총 환불된 금액
            - pendingAmount: 대기 중인 환불 금액
            """)
    @GetMapping("/stats")
    public ApiResponse<RefundStatsResponse> getRefundStats() {
        log.info("환불 통계 조회");

        RefundStatsResponse stats = RefundStatsResponse.builder()
                .pendingCount(adminRefundService.countByStatus("PENDING"))
                .completedCount(adminRefundService.countByStatus("COMPLETED"))
                .rejectedCount(adminRefundService.countByStatus("REJECTED"))
                .totalRefundedAmount(adminRefundService.sumRefundAmountByStatus("COMPLETED"))
                .pendingAmount(adminRefundService.sumRefundAmountByStatus("PENDING"))
                .build();

        return ApiResponse.success(stats);
    }

    /**
     * 환불 통계 응답 DTO (내부 클래스)
     */
    @lombok.Getter
    @lombok.Builder
    public static class RefundStatsResponse {
        private long pendingCount;
        private long completedCount;
        private long rejectedCount;
        private java.math.BigDecimal totalRefundedAmount;
        private java.math.BigDecimal pendingAmount;
    }
}
