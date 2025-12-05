package com.hip.damoa.domain.ad.web.dto;

import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.ad.model.AdPayment;
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
public class AdCampaignResponse {

    private UUID uuid;
    private UUID companyUuid;
    private String companyName;
    private String name;
    private String description;
    private String adType;
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private Integer remainingDays;

    private BigDecimal totalSpent;

    /**
     * 총 1일 가치 (모든 활성 결제의 dailyValue 합)
     * 예: 결제1(100원/일) + 결제2(100원/일) = 200원/일
     */
    private BigDecimal totalDailyValue;

    /**
     * 우선순위 점수 (= totalDailyValue)
     */
    private BigDecimal priorityScore;
    private BigDecimal secondaryScore;

    private Boolean autoRenew;

    /**
     * 누적 결제 금액 (초기 + 추가 결제)
     * 다음 자동 갱신 시 이 금액으로 결제
     */
    private BigDecimal accumulatedPayment;

    /**
     * 현재 사이클 시작일
     */
    private LocalDate cycleStartDate;

    /**
     * 갱신 알림 발송 여부
     */
    private Boolean renewalNotified;

    private Long totalImpressions;
    private Long totalClicks;
    private Long totalConversions;

    private LocalDateTime createdAt;
    private LocalDateTime lastCalculatedAt;

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
