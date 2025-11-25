package com.hip.damoa.domain.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 업체 상세 통계 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyStatisticsResponse {

    private Long total;
    private Long newToday;
    private Long newThisMonth;

    // 상태별 통계 (ACTIVE, PENDING, INACTIVE, SUSPENDED)
    private Map<String, Long> byStatus;

    // 인증 여부
    private Long verified;

    // 프리미엄 업체 수
    private Long premium;

    // 프리미엄 등급별 통계 (NONE, BASIC, STANDARD, PREMIUM, VIP)
    private Map<String, Long> byPremiumTier;

    // 평균 평점
    private BigDecimal averageRating;

    // 총 리뷰 수
    private Long totalReviews;
}
