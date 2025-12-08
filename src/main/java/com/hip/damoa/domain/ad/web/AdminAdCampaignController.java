package com.hip.damoa.domain.ad.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.ad.model.AdPayment;
import com.hip.damoa.domain.ad.repository.AdCampaignRepository;
import com.hip.damoa.domain.ad.repository.AdPaymentRepository;
import com.hip.damoa.domain.ad.service.AdCampaignService;
import com.hip.damoa.domain.ad.web.dto.AdCampaignResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "1926. Admin - Ad Campaign", description = "관리자 광고 캠페인 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ad-campaigns")
public class AdminAdCampaignController {

    private final AdCampaignRepository campaignRepository;
    private final AdPaymentRepository paymentRepository;
    private final AdCampaignService campaignService;

    // ========== 캠페인 목록 조회 ==========

    @Operation(summary = "전체 캠페인 목록 조회", description = """
            모든 광고 캠페인 목록을 조회합니다.

            ## 필터 옵션
            - status: DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED
            - companyName: 회사명 검색 (부분 일치)

            ## 정렬
            - 기본값: createdAt DESC (최신순)
            - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
            - 기타 옵션: priorityScore, startDate, endDate
            """)
    @GetMapping
    public ApiResponse<Page<AdCampaignResponse>> getAllCampaigns(
            @Parameter(description = "상태 필터") @RequestParam(required = false) String status,
            @Parameter(description = "회사명 검색") @RequestParam(required = false) String companyName,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AdCampaign> campaigns;

        if (status != null && companyName != null) {
            campaigns = campaignRepository.findByStatusAndCompanyNameContainingAndIsDeletedFalse(
                    status, companyName, pageable);
        } else if (status != null) {
            campaigns = campaignRepository.findByStatusAndIsDeletedFalse(status, pageable);
        } else if (companyName != null) {
            campaigns = campaignRepository.findByCompanyNameContainingAndIsDeletedFalse(companyName, pageable);
        } else {
            campaigns = campaignRepository.findByIsDeletedFalse(pageable);
        }

        return ApiResponse.success(campaigns.map(campaign -> {
            List<AdPayment> payments = paymentRepository.findByCampaignAndIsDeletedFalse(campaign);
            return AdCampaignResponse.from(campaign, payments);
        }));
    }

    @Operation(summary = "활성 캠페인 목록 조회", description = "현재 활성 상태인 캠페인을 우선순위 순으로 조회합니다.")
    @GetMapping("/active")
    public ApiResponse<List<AdCampaignResponse>> getActiveCampaigns() {
        LocalDate today = LocalDate.now();
        List<AdCampaign> campaigns = campaignRepository.findActiveCampaignsByPriority(today);

        List<AdCampaignResponse> responses = campaigns.stream()
                .map(campaign -> {
                    List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), today);
                    return AdCampaignResponse.from(campaign, payments);
                })
                .toList();

        return ApiResponse.success(responses);
    }

    // ========== 캠페인 상세 조회 ==========

    @Operation(summary = "캠페인 상세 조회", description = "특정 캠페인의 상세 정보와 결제 내역을 조회합니다.")
    @GetMapping("/{campaignUuid}")
    public ApiResponse<AdCampaignResponse> getCampaign(
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid) {
        return ApiResponse.success(campaignService.getCampaignForAdmin(campaignUuid));
    }

    // ========== 캠페인 상태 관리 ==========

    @Operation(summary = "캠페인 상태 변경", description = """
            캠페인 상태를 변경합니다.

            **가능한 상태**:
            - ACTIVE: 활성화
            - PAUSED: 일시정지
            - COMPLETED: 완료
            - CANCELLED: 취소
            """)
    @PatchMapping("/{campaignUuid}/status")
    public ApiResponse<AdCampaignResponse> updateCampaignStatus(
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid,
            @Parameter(description = "변경할 상태") @RequestParam String status) {

        AdCampaign campaign = campaignRepository.findByUuidAndIsDeletedFalse(campaignUuid)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다"));

        switch (status.toUpperCase()) {
            case "ACTIVE" -> campaign.activate();
            case "PAUSED" -> campaign.pause();
            case "COMPLETED" -> campaign.complete();
            case "CANCELLED" -> campaign.cancel();
            default -> throw new RuntimeException("잘못된 상태: " + status);
        }

        campaign = campaignRepository.save(campaign);

        log.info("캠페인 상태 변경: campaignId={}, newStatus={}", campaign.getId(), status);

        List<AdPayment> payments = paymentRepository.findByCampaignAndIsDeletedFalse(campaign);
        return ApiResponse.success(AdCampaignResponse.from(campaign, payments));
    }

    @Operation(summary = "자동 갱신 설정 변경", description = "캠페인의 자동 갱신 설정을 변경합니다.")
    @PatchMapping("/{campaignUuid}/auto-renew")
    public ApiResponse<AdCampaignResponse> updateAutoRenew(
            @Parameter(description = "캠페인 UUID") @PathVariable UUID campaignUuid,
            @Parameter(description = "자동 갱신 설정") @RequestParam boolean autoRenew) {

        AdCampaign campaign = campaignRepository.findByUuidAndIsDeletedFalse(campaignUuid)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다"));

        campaign.setAutoRenew(autoRenew);
        campaign = campaignRepository.save(campaign);

        log.info("캠페인 자동 갱신 설정 변경: campaignId={}, autoRenew={}", campaign.getId(), autoRenew);

        List<AdPayment> payments = paymentRepository.findByCampaignAndIsDeletedFalse(campaign);
        return ApiResponse.success(AdCampaignResponse.from(campaign, payments));
    }

    // ========== 수동 작업 ==========

    @Operation(summary = "우선순위 재계산", description = "모든 활성 캠페인의 우선순위를 재계산합니다.")
    @PostMapping("/recalculate-priorities")
    public ApiResponse<Map<String, Object>> recalculatePriorities() {
        int count = campaignService.recalculateAllPriorities();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "우선순위 재계산 완료");
        result.put("updatedCount", count);

        return ApiResponse.success(result);
    }

    @Operation(summary = "만료 캠페인 처리", description = "만료된 캠페인을 완료 상태로 처리합니다.")
    @PostMapping("/process-expired")
    public ApiResponse<Map<String, Object>> processExpiredCampaigns() {
        int campaignCount = campaignService.completeExpiredCampaigns();
        int paymentCount = campaignService.consumeExpiredPayments();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "만료 캠페인 처리 완료");
        result.put("completedCampaigns", campaignCount);
        result.put("consumedPayments", paymentCount);

        return ApiResponse.success(result);
    }

    @Operation(summary = "자동 갱신 처리", description = "자동 갱신 대상 캠페인을 수동으로 처리합니다.")
    @PostMapping("/process-auto-renewals")
    public ApiResponse<Map<String, Object>> processAutoRenewals() {
        int count = campaignService.processAutoRenewals();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "자동 갱신 처리 완료");
        result.put("renewedCount", count);

        return ApiResponse.success(result);
    }

    @Operation(summary = "갱신 알림 발송", description = "갱신 예정 캠페인에 알림을 발송합니다.")
    @PostMapping("/send-renewal-notifications")
    public ApiResponse<Map<String, Object>> sendRenewalNotifications() {
        int count = campaignService.sendRenewalNotifications();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "갱신 알림 발송 완료");
        result.put("notifiedCount", count);

        return ApiResponse.success(result);
    }

    // ========== 통계 ==========

    @Operation(summary = "캠페인 통계 조회", description = "캠페인 전체 통계를 조회합니다.")
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCampaigns", campaignRepository.countByIsDeletedFalse());
        stats.put("activeCampaigns", campaignRepository.countByStatusAndIsDeletedFalse("ACTIVE"));
        stats.put("pausedCampaigns", campaignRepository.countByStatusAndIsDeletedFalse("PAUSED"));
        stats.put("completedCampaigns", campaignRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        stats.put("cancelledCampaigns", campaignRepository.countByStatusAndIsDeletedFalse("CANCELLED"));

        // 자동 갱신 설정 캠페인 수
        stats.put("autoRenewEnabled", campaignRepository.countByAutoRenewAndStatusAndIsDeletedFalse(true, "ACTIVE"));

        return ApiResponse.success(stats);
    }
}
