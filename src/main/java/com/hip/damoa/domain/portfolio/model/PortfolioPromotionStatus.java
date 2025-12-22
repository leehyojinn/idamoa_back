package com.hip.damoa.domain.portfolio.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 프로모션 상태
 */
@Getter
@RequiredArgsConstructor
public enum PortfolioPromotionStatus {

    ACTIVE("활성"),
    EXPIRED("만료"),
    CANCELLED("취소");

    private final String displayName;

    /**
     * 문자열로부터 enum 변환
     */
    public static PortfolioPromotionStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PortfolioPromotionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
