package com.hip.damoa.domain.ad.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.ad.service.AdCampaignService;
import com.hip.damoa.domain.ad.web.dto.*;
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
import java.util.UUID;

@Tag(name = "1005-2. Ad Campaign", description = "광고 캠페인 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ad-campaigns")
public class AdCampaignController {

    private final AdCampaignService adCampaignService;

    // ========== 캠페인 생성/결제 API ==========

    @Operation(summary = "광고 캠페인 생성", description = """
            새로운 광고 캠페인을 생성합니다.

            **광고 기간 옵션**:
            - 7일: 최소 3,500원 (500원/일 × 7일)
            - 14일: 최소 7,000원 (500원/일 × 14일)
            - 30일: 최소 15,000원 (500원/일 × 30일)

            **우선순위 계산**:
            - 1일 가치 = Σ(결제금액 / 결제시점 남은일수)
            - 일당 가치가 높을수록 상위 노출
            - 동점 시 회사 평점, 리뷰수, 선등록 순으로 정렬

            **자동 갱신**:
            - autoRenew=true 설정 시 종료일에 자동 갱신
            - 갱신 금액 = 누적 결제 금액 (초기 + 추가 결제)
            - 크레딧 부족 시 자동 갱신 중단
            - 갱신 3일 전 알림 발송

            **주의사항**:
            - 크레딧이 결제 금액만큼 차감됩니다.
            - 회사당 하나의 활성 캠페인만 가질 수 있습니다.
            """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdCampaignResponse> createCampaign(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AdCampaignCreateRequest request) {
        return ApiResponse.success(adCampaignService.createCampaign(
                userDetails.getUsername(), request));
    }

    @Operation(summary = "광고 추가 결제", description = """
            활성 캠페인에 추가 결제하여 우선순위를 높입니다.

            **효과**:
            - 추가 결제 시 즉시 우선순위 점수가 재계산됩니다.
            - 1일 가치 = Σ(결제금액 / 결제시점 남은일수)
            - 추가 결제금액은 다음 자동 갱신 시에도 적용됩니다.

            **예시**:
            - 7일간 3,500원 결제 → 일당 500원
            - 2일차에 3,500원 추가 결제 (남은 6일) → 추가 일당 583원
            - 총 일당 가치 = 500원 + 583원 = 1,083원
            """)
    @PostMapping("/{campaignUuid}/payments")
    public ApiResponse<AdCampaignResponse> addPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid,
            @Valid @RequestBody AdPaymentRequest request) {
        return ApiResponse.success(adCampaignService.addPayment(
                userDetails.getUsername(), campaignUuid, request));
    }

    @Operation(summary = "자동 갱신 설정 변경", description = """
            광고 캠페인의 자동 갱신 설정을 켜거나 끕니다.

            **자동 갱신 규칙**:
            - 켜면: 캠페인 종료일에 누적 결제 금액(초기 + 추가)으로 자동 갱신
            - 끄면: 캠페인 종료일에 종료됨 (환불 없음)
            - 갱신 3일 전 알림 발송
            - 크레딧 부족 시 자동 갱신 중단
            """)
    @PatchMapping("/{campaignUuid}/auto-renew")
    public ApiResponse<AdCampaignResponse> toggleAutoRenew(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid,
            @Parameter(description = "자동 갱신 설정") @RequestParam boolean autoRenew) {
        return ApiResponse.success(adCampaignService.toggleAutoRenew(
                userDetails.getUsername(), campaignUuid, autoRenew));
    }

    // ========== 캠페인 조회 API ==========

    @Operation(summary = "내 활성 캠페인 조회", description = """
            현재 로그인한 사용자의 활성 광고 캠페인을 조회합니다.

            ## Response 필드 설명
            | 필드 | 설명 |
            |------|------|
            | uuid | 캠페인 고유 식별자 |
            | companyUuid | 업체 UUID |
            | companyName | 업체명 |
            | name | 캠페인명 |
            | description | 캠페인 설명 |
            | adType | 광고 유형 (SEARCH, FEATURED, BANNER) |
            | status | 캠페인 상태 (PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED) |
            | startDate | 시작일 |
            | endDate | 종료일 |
            | durationDays | 총 기간 (일) |
            | remainingDays | 남은 기간 (일) |
            | totalSpent | 총 지출 금액 (원) |
            | totalDailyValue | 총 1일 가치 (원/일) |
            | priorityScore | 우선순위 점수 (= totalDailyValue) |
            | secondaryScore | 부차 우선순위 점수 (등록 순서) |
            | autoRenew | 자동 갱신 여부 |
            | accumulatedPayment | 누적 결제 금액 (다음 갱신 시 적용) |
            | cycleStartDate | 현재 사이클 시작일 |
            | renewalNotified | 갱신 알림 발송 여부 |
            | totalImpressions | 총 노출 수 |
            | totalClicks | 총 클릭 수 |
            | totalConversions | 총 전환 수 |
            | createdAt | 생성일시 |
            | lastCalculatedAt | 마지막 우선순위 계산일시 |
            | payments | 결제 내역 목록 |
            """)
    @GetMapping("/my")
    public ApiResponse<AdCampaignResponse> getMyCampaign(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(adCampaignService.getMyCampaign(userDetails.getUsername()));
    }

    @Operation(summary = "내 캠페인 이력 조회", description = """
            현재 로그인한 사용자의 모든 광고 캠페인 이력을 조회합니다.

            ## Response 필드 설명 (AdCampaignResponse)
            | 필드 | 설명 |
            |------|------|
            | uuid | 캠페인 고유 식별자 |
            | companyUuid | 업체 UUID |
            | companyName | 업체명 |
            | name | 캠페인명 |
            | adType | 광고 유형 (SEARCH, FEATURED, BANNER) |
            | status | 캠페인 상태 (PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED) |
            | startDate | 시작일 |
            | endDate | 종료일 |
            | durationDays | 총 기간 (일) |
            | remainingDays | 남은 기간 (일) |
            | totalSpent | 총 지출 금액 (원) |
            | priorityScore | 우선순위 점수 |
            | autoRenew | 자동 갱신 여부 |
            | accumulatedPayment | 누적 결제 금액 |
            | createdAt | 생성일시 |
            | payments | 결제 내역 목록 |

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: startDate, endDate, status
            """)
    @GetMapping("/my/history")
    public ApiResponse<Page<AdCampaignResponse>> getMyCampaignHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adCampaignService.getMyCampaignHistory(
                userDetails.getUsername(), pageable));
    }

    @Operation(summary = "캠페인 상세 조회", description = """
            특정 광고 캠페인의 상세 정보를 조회합니다.

            **주의**: 캠페인 소유자만 조회할 수 있습니다.

            ## Response 필드 설명 (AdCampaignResponse)
            | 필드 | 설명 |
            |------|------|
            | uuid | 캠페인 고유 식별자 |
            | companyUuid | 업체 UUID |
            | companyName | 업체명 |
            | name | 캠페인명 |
            | description | 캠페인 설명 |
            | adType | 광고 유형 (SEARCH, FEATURED, BANNER) |
            | status | 캠페인 상태 (PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED) |
            | startDate | 시작일 |
            | endDate | 종료일 |
            | durationDays | 총 기간 (일) |
            | remainingDays | 남은 기간 (일) |
            | totalSpent | 총 지출 금액 (원) |
            | totalDailyValue | 총 1일 가치 (원/일) |
            | priorityScore | 우선순위 점수 (= totalDailyValue) |
            | secondaryScore | 부차 우선순위 점수 (등록 순서) |
            | autoRenew | 자동 갱신 여부 |
            | accumulatedPayment | 누적 결제 금액 (다음 갱신 시 적용) |
            | cycleStartDate | 현재 사이클 시작일 |
            | renewalNotified | 갱신 알림 발송 여부 |
            | totalImpressions | 총 노출 수 |
            | totalClicks | 총 클릭 수 |
            | totalConversions | 총 전환 수 |
            | createdAt | 생성일시 |
            | lastCalculatedAt | 마지막 우선순위 계산일시 |
            | payments | 결제 내역 목록 |

            ## payments 필드 설명 (AdPaymentResponse)
            | 필드 | 설명 |
            |------|------|
            | uuid | 결제 UUID |
            | paymentAmount | 결제 금액 (원) |
            | paymentDate | 결제일 |
            | applyFromDate | 적용 시작일 |
            | applyToDate | 적용 종료일 |
            | applyDays | 적용 일수 |
            | dailyValue | 1일 가치 (결제금액/적용일수) |
            | paymentType | 결제 유형 (INITIAL, ADDITIONAL) |
            | status | 결제 상태 (ACTIVE, CONSUMED, REFUNDED) |
            | remainingDays | 남은 일수 |
            | createdAt | 생성일시 |
            """)
    @GetMapping("/{campaignUuid}")
    public ApiResponse<AdCampaignResponse> getCampaign(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid) {
        return ApiResponse.success(adCampaignService.getCampaign(
                userDetails.getUsername(), campaignUuid));
    }

    // ========== 캠페인 취소 API ==========

    @Operation(summary = "광고 캠페인 취소", description = """
            광고 캠페인의 자동 갱신을 취소합니다.

            **주의사항**:
            - 현재 사이클은 끝까지 진행됩니다 (환불 없음)
            - 다음 자동 갱신만 중단됩니다 (autoRenew = false)
            - 캠페인은 종료일까지 정상 노출됩니다
            """)
    @DeleteMapping("/{campaignUuid}")
    public ApiResponse<AdCampaignResponse> cancelCampaign(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid) {
        return ApiResponse.success(
                adCampaignService.cancelCampaign(userDetails.getUsername(), campaignUuid));
    }
}
