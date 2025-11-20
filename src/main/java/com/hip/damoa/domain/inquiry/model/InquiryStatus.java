package com.hip.damoa.domain.inquiry.model;

/**
 * 문의 처리 상태
 */
public enum InquiryStatus {
    PENDING,      // 접수 대기
    IN_PROGRESS,  // 처리 중
    COMPLETED,    // 처리 완료
    CANCELLED     // 취소됨
}
