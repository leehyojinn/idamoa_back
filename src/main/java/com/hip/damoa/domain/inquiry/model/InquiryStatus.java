package com.hip.damoa.domain.inquiry.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일반 문의 처리 상태
 */
@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    PENDING("대기중"),      // 접수 대기
    IN_PROGRESS("처리중"),  // 처리 중
    ANSWERED("답변완료"),   // 답변 완료
    CLOSED("종료"),         // 종료
    COMPLETED("완료"),      // 완료 (개발서버 호환)
    CANCELLED("취소");      // 취소 (개발서버 호환)

    private final String description;
}
