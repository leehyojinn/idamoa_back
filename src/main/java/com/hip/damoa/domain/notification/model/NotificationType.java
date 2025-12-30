package com.hip.damoa.domain.notification.model;

/**
 * 알림 타입
 */
public enum NotificationType {
    /** 채팅 메시지 */
    CHAT_MESSAGE,
    /** 견적 요청 */
    ESTIMATE_REQUEST,
    /** 견적 응답 */
    ESTIMATE_RESPONSE,
    /** 시스템 공지 */
    SYSTEM_NOTICE,
    /** 마케팅 */
    MARKETING
}
