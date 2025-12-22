package com.hip.damoa.domain.portfolio.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 프로모션 타입 enum
 * - 기본 타입 정의 (확장은 DB 설정으로)
 */
@Getter
@RequiredArgsConstructor
public enum PortfolioPromotionType {

    STANDARD("일반우대", 1),
    PREMIUM("강력우대", 3);

    private final String displayName;
    private final int defaultWeight;

    /**
     * 문자열로부터 enum 변환
     */
    public static PortfolioPromotionType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PortfolioPromotionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
