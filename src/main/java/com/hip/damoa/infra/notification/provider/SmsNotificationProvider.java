package com.hip.damoa.infra.notification.provider;

import com.hip.damoa.domain.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SMS 알림 Provider (ALIGO, NCP 지원 예정)
 */
@Slf4j
@Component
public class SmsNotificationProvider implements NotificationProvider {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:ALIGO}")
    private String provider;

    @Override
    public String send(String recipient, String title, String content, Map<String, String> variables) {
        if (!isEnabled()) {
            log.warn("SMS 발송 비활성화 상태: recipient={}", recipient);
            return null;
        }

        // TODO: 실제 SMS 발송 구현
        // ALIGO: https://smartsms.aligo.in/admin/api/spec.html
        // NCP: https://api.ncloud-docs.com/docs/ai-application-service-sens-smsv2

        log.info("SMS 발송 (Mock): provider={}, recipient={}, content={}", provider, recipient, content);
        return "mock-sms-" + System.currentTimeMillis();
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isEnabled() {
        return smsEnabled;
    }
}
