package com.hip.damoa.domain.partnership.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 제휴/광고 문의 처리 상태
 */
@Getter
@RequiredArgsConstructor
public enum PartnershipStatus {
    PENDING("대기중"),      // 접수 대기
    IN_PROGRESS("처리중"),  // 처리 중
    COMPLETED("완료"),      // 처리 완료
    CANCELLED("취소");      // 취소됨

    private final String description;
}