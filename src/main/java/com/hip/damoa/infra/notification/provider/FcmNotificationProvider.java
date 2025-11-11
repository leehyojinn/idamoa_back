package com.hip.damoa.infra.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FCM (Firebase Cloud Messaging) Push 알림 Provider (향후 구현)
 */
@Slf4j
@Component
public class FcmNotificationProvider implements NotificationProvider {

    @Value("${notification.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Override
    public String send(String recipient, String title, String content, Map<String, String> variables) {
        if (!isEnabled()) {
            log.warn("FCM 푸시 발송 비활성화 상태: recipient={}", recipient);
            return null;
        }

        // TODO: FCM API 연동
        // https://firebase.google.com/docs/cloud-messaging

        log.info("FCM 푸시 발송 (Mock): fcmToken={}, title={}, content={}", recipient, title, content);
        return "mock-fcm-" + System.currentTimeMillis();
    }

    @Override
    public String getChannelType() {
        return "FCM";
    }

    @Override
    public boolean isEnabled() {
        return fcmEnabled;
    }
}
