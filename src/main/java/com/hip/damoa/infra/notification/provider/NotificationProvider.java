package com.hip.damoa.infra.notification.provider;

import java.util.Map;

/**
 * 통합 메시징 서비스 (UMS) Provider 인터페이스
 *
 * 지원 채널:
 * - EMAIL: Gmail API
 * - SMS: ALIGO, NCP
 * - KAKAO: 카카오 알림톡
 * - FCM: Firebase Cloud Messaging
 */
public interface NotificationProvider {

    /**
     * 메시지 발송
     *
     * @param recipient 수신자 (이메일 주소, 전화번호, FCM 토큰 등)
     * @param title     제목 (이메일, 푸시 등에서 사용)
     * @param content   본문
     * @param variables 추가 변수 (템플릿 치환용)
     * @return 발송 결과 ID (provider message ID)
     */
    String send(String recipient, String title, String content, Map<String, String> variables);

    /**
     * Provider가 지원하는 채널 타입
     *
     * @return EMAIL, SMS, KAKAO, FCM 등
     */
    String getChannelType();

    /**
     * Provider 활성화 여부
     *
     * @return true if enabled
     */
    boolean isEnabled();
}
