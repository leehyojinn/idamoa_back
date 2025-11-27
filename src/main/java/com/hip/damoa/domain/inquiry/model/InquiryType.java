package com.hip.damoa.domain.inquiry.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일반 문의 유형
 */
@Getter
@RequiredArgsConstructor
public enum InquiryType {
    BUG("버그 신고"),
    PAYMENT_ERROR("결제 오류"),
    ACCOUNT_ISSUE("계정 문제"),
    SUGGESTION("건의사항"),
    OTHER("기타");

    private final String description;
}
