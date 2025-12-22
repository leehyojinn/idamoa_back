package com.hip.damoa.domain.portfolio.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 프로모션 결제 유형
 */
@Getter
@RequiredArgsConstructor
public enum PortfolioPromotionPaymentType {

    INITIAL("최초 등록"),
    RENEWAL("갱신"),
    UPGRADE("업그레이드");

    private final String displayName;
}
