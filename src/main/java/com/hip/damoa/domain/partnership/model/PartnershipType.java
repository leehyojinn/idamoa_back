package com.hip.damoa.domain.partnership.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 제휴/광고 문의 유형
 */
@Getter
@RequiredArgsConstructor
public enum PartnershipType {
    PARTNERSHIP("제휴 문의"),
    ADVERTISEMENT("광고 문의"),
    OTHER("기타");

    private final String description;
}