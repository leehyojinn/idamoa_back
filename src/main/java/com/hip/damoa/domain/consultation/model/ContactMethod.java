package com.hip.damoa.domain.consultation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 연락 방법
 */
@Getter
@RequiredArgsConstructor
public enum ContactMethod {
    PHONE("전화"),
    EMAIL("이메일"),
    KAKAO("카카오톡"),
    ANY("무관");

    private final String description;
}
