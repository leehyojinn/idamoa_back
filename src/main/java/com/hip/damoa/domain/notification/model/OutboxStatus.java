package com.hip.damoa.domain.notification.model;

/**
 * 알림 Outbox 상태
 */
public enum OutboxStatus {
    /** 대기 중 */
    PENDING,
    /** 처리 중 */
    PROCESSING,
    /** 발송 완료 */
    SENT,
    /** 발송 실패 */
    FAILED,
    /** 취소됨 */
    CANCELLED
}
