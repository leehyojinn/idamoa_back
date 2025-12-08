package com.hip.damoa.domain.payment.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.payment.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05-1. Credit", description = "크레딧 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService creditService;

    // ========== 조회 API ==========

    @Operation(summary = "크레딧 잔액 조회", description = "현재 사용자의 크레딧 잔액을 조회합니다.")
    @GetMapping("/balance")
    public ApiResponse<CreditBalanceResponse> getBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(creditService.getBalance(userDetails.getUsername()));
    }

    @Operation(summary = "충전 패키지 목록 조회", description = """
            구매 가능한 크레딧 패키지 목록을 조회합니다.

            **패키지 종류**:
            - 1만원: x1~x4 (보너스 없음)
            - 3만원: x1~x4 (5%~8% 보너스)
            - 5만원: x1~x4 (6%~10% 보너스)
            - 10만원: x1~x4 (10% 보너스, 최대 40,000 크레딧)
            """)
    @GetMapping("/packages")
    public ApiResponse<List<CreditPackageResponse>> getPackages() {
        return ApiResponse.success(creditService.getAvailablePackages());
    }

    @Operation(summary = "크레딧 거래 내역 조회", description = """
            크레딧 충전/사용/환불 내역을 조회합니다.

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: amount, transactionType
            """)
    @GetMapping("/transactions")
    public ApiResponse<Page<CreditTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(creditService.getTransactions(userDetails.getUsername(), pageable));
    }

    // ========== 충전 API ==========

    @Operation(summary = "크레딧 충전 요청", description = """
            크레딧 충전 결제를 시작합니다.

            **패키지 코드 예시**:
            - PACK_10000_X1: 1만원 x1 (보너스 없음)
            - PACK_30000_X2: 3만원 x2 = 6만원 (보너스 3,600원)
            - PACK_50000_X4: 5만원 x4 = 20만원 (보너스 20,000원)
            - PACK_100000_X1: 10만원 x1 (보너스 10,000원)

            **결제 수단**:
            - KAKAOPAY: 카카오페이
            - TOSS: 토스페이먼츠
            - CARD: 신용/체크카드

            응답의 paymentUrl로 리다이렉트하여 결제를 진행합니다.
            """)
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreditPurchaseResponse> purchaseCredits(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreditPurchaseRequest request) {
        return ApiResponse.success(creditService.initiateCreditPurchase(
                userDetails.getUsername(), request));
    }

    @Operation(summary = "크레딧 충전 완료 콜백", description = """
            PG사 결제 완료 후 호출되는 콜백 API입니다.

            프론트엔드에서 결제 완료 후 이 API를 호출하여 크레딧을 적립합니다.
            """)
    @PostMapping("/purchase/complete")
    public ApiResponse<Void> completePurchase(
            @Parameter(description = "주문 ID") @RequestParam String orderId,
            @Parameter(description = "PG 토큰") @RequestParam String pgToken) {
        creditService.completeCreditPurchase(orderId, pgToken);
        return ApiResponse.success();
    }

    // ========== 환불 API ==========

    @Operation(summary = "크레딧 환불 요청", description = """
            미사용 크레딧 환불을 요청합니다.

            **환불 정책**:
            - 최소 환불 금액: 1,000원
            - 환불 수수료: 10% (100원 단위 올림)
            - 환불 계좌 정보 필수
            - 처리 기간: 영업일 기준 3~5일

            **예시**:
            - 10,000원 환불 요청 시 → 수수료 1,000원 → 실제 환불 9,000원
            - 5,000원 환불 요청 시 → 수수료 500원 → 실제 환불 4,500원
            """)
    @PostMapping("/refund")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreditRefundResponse> requestRefund(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreditRefundRequest request) {
        return ApiResponse.success(creditService.requestRefund(
                userDetails.getUsername(), request));
    }
}
