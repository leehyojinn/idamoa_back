package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.ad.model.AdPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 광고 캠페인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "광고 캠페인 응답")
public class AdCampaignResponse {

    @Schema(description = "캠페인 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    @Schema(description = "캠페인명", example = "2025년 신년 프로모션 광고")
    private String name;

    @Schema(description = "캠페인 설명", example = "신년 맞이 인테리어 할인 광고")
    private String description;

    @Schema(description = "광고 유형 (SEARCH, FEATURED, BANNER)", example = "FEATURED")
    private String adType;

    @Schema(description = "캠페인 상태 (PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED)", example = "ACTIVE")
    private String status;

    @Schema(description = "시작일", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "종료일", example = "2025-01-31")
    private LocalDate endDate;

    @Schema(description = "총 기간 (일)", example = "30")
    private Integer durationDays;

    @Schema(description = "남은 기간 (일)", example = "15")
    private Integer remainingDays;

    @Schema(description = "총 지출 금액 (원)", example = "50000")
    private BigDecimal totalSpent;

    @Schema(description = "총 1일 가치 (원/일)", example = "1666.67")
    private BigDecimal totalDailyValue;

    @Schema(description = "우선순위 점수", example = "1666.67")
    private BigDecimal priorityScore;

    @Schema(description = "부차 우선순위 점수 (등록 순서)", example = "1000000000")
    private BigDecimal secondaryScore;

    @Schema(description = "자동 갱신 여부", example = "true")
    private Boolean autoRenew;

    @Schema(description = "누적 결제 금액 (원)", example = "50000")
    private BigDecimal accumulatedPayment;

    @Schema(description = "현재 사이클 시작일", example = "2025-01-01")
    private LocalDate cycleStartDate;

    @Schema(description = "갱신 알림 발송 여부", example = "false")
    private Boolean renewalNotified;

    @Schema(description = "총 노출 수", example = "1500")
    private Long totalImpressions;

    @Schema(description = "총 클릭 수", example = "120")
    private Long totalClicks;

    @Schema(description = "총 전환 수", example = "25")
    private Long totalConversions;

    @Schema(description = "생성일시", example = "2025-01-01T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "마지막 우선순위 계산일시", example = "2025-01-15T00:05:00")
    private LocalDateTime lastCalculatedAt;

    @Schema(description = "결제 내역 목록")
    private List<AdPaymentResponse> payments;

    public static AdCampaignResponse from(AdCampaign campaign, List<AdPayment> payments) {
        return AdCampaignResponse.builder()
                .uuid(campaign.getUuid())
                .companyUuid(campaign.getCompany().getUuid())
                .companyName(campaign.getCompany().getName())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .adType(campaign.getAdType().name())
                .status(campaign.getStatus())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .durationDays(campaign.getDurationDays())
                .remainingDays(campaign.getRemainingDays())
                .totalSpent(campaign.getTotalSpent())
                .totalDailyValue(campaign.getTotalDailyValue())
                .priorityScore(campaign.getPriorityScore())
                .secondaryScore(campaign.getSecondaryScore())
                .autoRenew(campaign.getAutoRenew())
                .accumulatedPayment(campaign.getAccumulatedPayment())
                .cycleStartDate(campaign.getCycleStartDate())
                .renewalNotified(campaign.getRenewalNotified())
                .totalImpressions(campaign.getTotalImpressions())
                .totalClicks(campaign.getTotalClicks())
                .totalConversions(campaign.getTotalConversions())
                .createdAt(campaign.getCreatedAt())
                .lastCalculatedAt(campaign.getLastCalculatedAt())
                .payments(payments.stream()
                        .map(AdPaymentResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
