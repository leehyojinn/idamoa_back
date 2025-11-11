package com.hip.damoa.infra.notification.provider;

import com.hip.damoa.domain.notification.model.NotificationChannel;
import com.hip.damoa.infra.notification.GmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 이메일 알림 Provider (Gmail API 사용)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.email.gmail-api.enabled", havingValue = "true")
public class EmailNotificationProvider implements NotificationProvider {

    private GmailService gmailService;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Autowired(required = false)
    public void setGmailService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @Override
    public String send(String recipient, String title, String content, Map<String, String> variables) {
        if (!isEnabled()) {
            log.warn("이메일 발송 비활성화 상태: recipient={}", recipient);
            return null;
        }

        if (gmailService == null) {
            log.error("GmailService가 초기화되지 않았습니다");
            throw new IllegalStateException("Gmail API가 활성화되지 않았습니다");
        }

        try {
            String messageId = gmailService.sendHtmlEmail(recipient, title, content);
            log.info("이메일 발송 성공: recipient={}, messageId={}", recipient, messageId);
            return messageId;
        } catch (Exception e) {
            log.error("이메일 발송 실패: recipient={}, error={}", recipient, e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Override
    public NotificationChannel getChannelType() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return emailEnabled && gmailService != null;
    }
}
