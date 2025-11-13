package com.hip.damoa.domain.consultation.model;

/**
 * 빠른상담 상태
 */
public enum ConsultationStatus {
    SUBMITTED,      // 신청완료 (상담 접수됨)
    IN_PROGRESS,    // 상담중 (진행 중)
    COMPLETED,      // 상담완료 (완료됨)
    CANCELLED       // 취소 (취소됨)
}
