package com.hip.damoa.infra.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 카카오 알림톡 Provider (향후 구현)
 */
@Slf4j
@Component
public class KakaoNotificationProvider implements NotificationProvider {

    @Value("${notification.kakao.enabled:false}")
    private boolean kakaoEnabled;

    @Override
    public String send(String recipient, String title, String content, Map<String, String> variables) {
        if (!isEnabled()) {
            log.warn("카카오 알림톡 발송 비활성화 상태: recipient={}", recipient);
            return null;
        }

        // TODO: 카카오 알림톡 API 연동
        // https://developers.kakao.com/docs/latest/ko/alimtalk/common

        log.info("카카오 알림톡 발송 (Mock): recipient={}, content={}", recipient, content);
        return "mock-kakao-" + System.currentTimeMillis();
    }

    @Override
    public String getChannelType() {
        return "KAKAO";
    }

    @Override
    public boolean isEnabled() {
        return kakaoEnabled;
    }
}
