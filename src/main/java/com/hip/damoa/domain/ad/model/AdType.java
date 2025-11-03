package com.hip.damoa.domain.ad.model;

/**
 * 광고 타입
 */
public enum AdType {
    LISTING,            // 상위 노출 광고 (입찰 기반)
    AI_RECOMMENDATION,  // AI 추천 가중치
    BANNER,             // 배너 광고 (위치, 기간 설정)
    POPUP               // 팝업 광고 (표시 규칙)
}
