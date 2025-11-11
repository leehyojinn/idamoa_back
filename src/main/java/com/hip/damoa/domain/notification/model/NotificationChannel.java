package com.hip.damoa.domain.notification.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 채널 타입
 *
 * EMAIL: 이메일 (Gmail API)
 * SMS: 문자 메시지 (ALIGO, NCP)
 * KAKAO: 카카오 알림톡
 * FCM: Firebase 푸시 알림
 */
@Getter
@RequiredArgsConstructor
public enum NotificationChannel {

    EMAIL("EMAIL", "이메일"),
    SMS("SMS", "문자 메시지"),
    KAKAO("KAKAO", "카카오 알림톡"),
    FCM("FCM", "푸시 알림");

    private final String code;
    private final String description;

    /**
     * code 값으로 Enum 찾기 (대소문자 무시)
     *
     * @param code 채널 코드 ("EMAIL", "email" 등)
     * @return NotificationChannel or null
     */
    public static NotificationChannel fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (NotificationChannel channel : values()) {
            if (channel.code.equalsIgnoreCase(code)) {
                return channel;
            }
        }

        return null;
    }

    /**
     * 안전한 valueOf (예외 대신 null 반환)
     *
     * @param name Enum 이름
     * @return NotificationChannel or null
     */
    public static NotificationChannel safeValueOf(String name) {
        if (name == null) {
            return null;
        }

        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
