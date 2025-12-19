package com.hip.damoa.domain.board.model;

import java.math.BigDecimal;

/**
 * 갤러리 우대 등록 타입
 */
public enum GalleryPromotionType {

    /**
     * 일반우대: 5만원/월, 가중치 1
     */
    STANDARD(new BigDecimal("50000"), 1),

    /**
     * 강력우대: 10만원/월, 가중치 3 (노출 확률 3배)
     */
    PREMIUM(new BigDecimal("100000"), 3);

    private final BigDecimal monthlyPrice;
    private final int weight;

    GalleryPromotionType(BigDecimal monthlyPrice, int weight) {
        this.monthlyPrice = monthlyPrice;
        this.weight = weight;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * STANDARD에서 PREMIUM으로 업그레이드 시 차액
     */
    public static BigDecimal getUpgradePrice() {
        return PREMIUM.monthlyPrice.subtract(STANDARD.monthlyPrice);
    }
}
