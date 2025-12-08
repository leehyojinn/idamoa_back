package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.AdminCreditService;
import com.hip.damoa.domain.payment.web.dto.AdminCreditAdjustRequest;
import com.hip.damoa.domain.payment.web.dto.AdminTransactionResponse;
import com.hip.damoa.domain.payment.web.dto.AdminUserCreditResponse;
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
 * 관리자용 크레딧 관리 컨트롤러
 */
@Slf4j
@Tag(name = "1922. Admin Credit", description = "관리자 크레딧 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/credits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCreditController {

    private final AdminCreditService adminCreditService;

    // ==================== 사용자 크레딧 조회 ====================

    @Operation(summary = "사용자 크레딧 목록 조회",
            description = """
            모든 사용자의 크레딧 정보를 페이징하여 조회합니다.

            **검색 옵션**
            - keyword: 이메일 또는 이름으로 검색

            ## 정렬
            - 기본값: availableCredits DESC (가용 크레딧 높은 순)
            - 사용법: sort=availableCredits,desc 또는 sort=availableCredits,asc
            - 기타 옵션: createdAt, totalSpent
            """)
    @GetMapping("/users")
    public ApiResponse<Page<AdminUserCreditResponse>> getUserCredits(
            @Parameter(description = "검색 키워드 (이메일/이름)") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "availableCredits", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("사용자 크레딧 목록 조회: keyword={}", keyword);

        Page<AdminUserCreditResponse> credits;
        if (keyword != null && !keyword.isBlank()) {
            credits = adminCreditService.searchUserCredits(keyword, pageable);
        } else {
            credits = adminCreditService.getAllUserCredits(pageable);
        }

        return ApiResponse.success(credits);
    }

    @Operation(summary = "특정 사용자 크레딧 조회",
            description = "특정 사용자의 크레딧 상세 정보를 조회합니다.")
    @GetMapping("/users/{userUuid}")
    public ApiResponse<AdminUserCreditResponse> getUserCredit(
            @Parameter(description = "사용자 UUID") @PathVariable UUID userUuid) {

        log.info("특정 사용자 크레딧 조회: userUuid={}", userUuid);
        AdminUserCreditResponse credit = adminCreditService.getUserCreditByUuid(userUuid);
        return ApiResponse.success(credit);
    }

    // ==================== 수동 크레딧 지급/차감 ====================

    @Operation(summary = "크레딧 지급 (관리자)",
            description = """
            관리자가 사용자에게 크레딧을 수동으로 지급합니다.

            **사용 예시**
            - 이벤트 당첨 크레딧 지급
            - 오류 보상 크레딧 지급
            - 프로모션 크레딧 지급

            **필수 정보**
            - amount: 지급할 크레딧 금액
            - reason: 지급 사유 (내역에 기록됨)
            """)
    @PostMapping("/users/{userUuid}/grant")
    public ApiResponse<AdminUserCreditResponse> grantCredits(
            @Parameter(description = "사용자 UUID") @PathVariable UUID userUuid,
            @Valid @RequestBody AdminCreditAdjustRequest request) {

        log.info("관리자 크레딧 지급: userUuid={}, amount={}", userUuid, request.getAmount());
        AdminUserCreditResponse credit = adminCreditService.grantCredits(userUuid, request);
        return ApiResponse.success(credit);
    }

    @Operation(summary = "크레딧 차감 (관리자)",
            description = """
            관리자가 사용자의 크레딧을 수동으로 차감합니다.

            **사용 예시**
            - 부정 사용 크레딧 회수
            - 환불 후 잔여 크레딧 조정

            **필수 정보**
            - amount: 차감할 크레딧 금액
            - reason: 차감 사유 (내역에 기록됨)

            **주의사항**
            - 잔액이 부족하면 오류가 발생합니다
            """)
    @PostMapping("/users/{userUuid}/deduct")
    public ApiResponse<AdminUserCreditResponse> deductCredits(
            @Parameter(description = "사용자 UUID") @PathVariable UUID userUuid,
            @Valid @RequestBody AdminCreditAdjustRequest request) {

        log.info("관리자 크레딧 차감: userUuid={}, amount={}", userUuid, request.getAmount());
        AdminUserCreditResponse credit = adminCreditService.deductCredits(userUuid, request);
        return ApiResponse.success(credit);
    }

    // ==================== 거래 내역 조회 ====================

    @Operation(summary = "전체 거래 내역 조회",
            description = """
            모든 크레딧 거래 내역을 페이징하여 조회합니다.

            **필터 옵션**
            - type: 거래 유형 (EARN, SPEND, REFUND, ADMIN_GRANT, ADMIN_DEDUCT)
            - keyword: 검색 (사용자 이메일/이름, 사유)

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: amount, transactionType
            """)
    @GetMapping("/transactions")
    public ApiResponse<Page<AdminTransactionResponse>> getTransactions(
            @Parameter(description = "거래 유형 필터") @RequestParam(required = false) String type,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("전체 거래 내역 조회: type={}, keyword={}", type, keyword);

        Page<AdminTransactionResponse> transactions;
        if (keyword != null && !keyword.isBlank()) {
            transactions = adminCreditService.searchTransactions(keyword, pageable);
        } else if (type != null && !type.isBlank()) {
            transactions = adminCreditService.getTransactionsByType(type, pageable);
        } else {
            transactions = adminCreditService.getAllTransactions(pageable);
        }

        return ApiResponse.success(transactions);
    }

    @Operation(summary = "특정 사용자 거래 내역 조회",
            description = """
            특정 사용자의 크레딧 거래 내역을 조회합니다.

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            """)
    @GetMapping("/users/{userUuid}/transactions")
    public ApiResponse<Page<AdminTransactionResponse>> getUserTransactions(
            @Parameter(description = "사용자 UUID") @PathVariable UUID userUuid,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("특정 사용자 거래 내역 조회: userUuid={}", userUuid);
        Page<AdminTransactionResponse> transactions = adminCreditService.getUserTransactions(userUuid, pageable);
        return ApiResponse.success(transactions);
    }
}
