package com.hip.damoa.domain.consultation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 상담신청 상태
 */
@Getter
@RequiredArgsConstructor
public enum PortfolioConsultationStatus {
    PENDING("대기중"),
    IN_PROGRESS("처리중"),
    ANSWERED("답변완료"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String description;
}
