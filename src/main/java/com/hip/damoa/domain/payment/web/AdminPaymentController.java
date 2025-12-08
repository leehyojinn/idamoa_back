package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.AdminPaymentService;
import com.hip.damoa.domain.payment.web.dto.AdminPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 결제 관리 컨트롤러
 */
@Slf4j
@Tag(name = "1924. Admin Payment", description = "관리자 결제 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    // ==================== 결제 내역 조회 ====================

    @Operation(summary = "결제 내역 조회",
            description = """
            모든 결제 내역을 페이징하여 조회합니다.

            **필터 옵션**
            - status: 결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED)
            - entityType: 결제 대상 유형 (CREDIT_PURCHASE, CREDIT_REFUND, AD_CAMPAIGN, FILE_DOWNLOAD)
            - keyword: 검색 (사용자 이메일/이름, 거래ID)
            - startDate, endDate: 기간 필터

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: amount, status
            """)
    @GetMapping
    public ApiResponse<Page<AdminPaymentResponse>> getPayments(
            @Parameter(description = "결제 상태 필터") @RequestParam(required = false) String status,
            @Parameter(description = "엔티티 타입 필터") @RequestParam(required = false) String entityType,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "시작일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("결제 내역 조회: status={}, entityType={}, keyword={}", status, entityType, keyword);

        Page<AdminPaymentResponse> payments;
        if (keyword != null && !keyword.isBlank()) {
            payments = adminPaymentService.searchPayments(keyword, pageable);
        } else if (startDate != null && endDate != null) {
            payments = adminPaymentService.getPaymentsByPeriod(startDate, endDate, pageable);
        } else if (entityType != null && !entityType.isBlank()) {
            payments = adminPaymentService.getPaymentsByEntityType(entityType, pageable);
        } else if (status != null && !status.isBlank()) {
            payments = adminPaymentService.getPaymentsByStatus(status, pageable);
        } else {
            payments = adminPaymentService.getAllPayments(pageable);
        }

        return ApiResponse.success(payments);
    }

    @Operation(summary = "결제 상세 조회",
            description = "특정 결제의 상세 정보를 조회합니다.")
    @GetMapping("/{paymentUuid}")
    public ApiResponse<AdminPaymentResponse> getPayment(
            @Parameter(description = "결제 UUID") @PathVariable UUID paymentUuid) {

        log.info("결제 상세 조회: uuid={}", paymentUuid);
        AdminPaymentResponse payment = adminPaymentService.getPayment(paymentUuid);
        return ApiResponse.success(payment);
    }

    @Operation(summary = "특정 사용자 결제 내역 조회",
            description = """
            특정 사용자의 결제 내역을 조회합니다.

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            """)
    @GetMapping("/users/{userUuid}")
    public ApiResponse<Page<AdminPaymentResponse>> getUserPayments(
            @Parameter(description = "사용자 UUID") @PathVariable UUID userUuid,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("특정 사용자 결제 내역 조회: userUuid={}", userUuid);
        Page<AdminPaymentResponse> payments = adminPaymentService.getUserPayments(userUuid, pageable);
        return ApiResponse.success(payments);
    }

    // ==================== 통계 ====================

    @Operation(summary = "결제 통계",
            description = """
            결제 관련 통계 정보를 조회합니다.

            **반환 정보**
            - completedCount: 완료된 결제 건수
            - completedAmount: 완료된 결제 총액
            - pendingAmount: 대기 중인 결제 금액
            - failedAmount: 실패한 결제 금액
            """)
    @GetMapping("/stats")
    public ApiResponse<PaymentStatsResponse> getPaymentStats() {
        log.info("결제 통계 조회");

        PaymentStatsResponse stats = PaymentStatsResponse.builder()
                .completedCount(adminPaymentService.countCompletedPayments())
                .completedAmount(adminPaymentService.sumPaymentAmountByStatus("COMPLETED"))
                .pendingAmount(adminPaymentService.sumPaymentAmountByStatus("PENDING"))
                .failedAmount(adminPaymentService.sumPaymentAmountByStatus("FAILED"))
                .build();

        return ApiResponse.success(stats);
    }

    /**
     * 결제 통계 응답 DTO (내부 클래스)
     */
    @lombok.Getter
    @lombok.Builder
    public static class PaymentStatsResponse {
        private long completedCount;
        private BigDecimal completedAmount;
        private BigDecimal pendingAmount;
        private BigDecimal failedAmount;
    }
}
