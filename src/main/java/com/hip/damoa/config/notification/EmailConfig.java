package com.hip.damoa.config.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Email Configuration
 *
 * Gmail API 방식을 사용하므로 SMTP 설정은 제거됨
 * Gmail API 설정은 GmailConfig.java 참고
 */
@Configuration
@EnableAsync
public class EmailConfig {
    // Gmail API 사용으로 SMTP 관련 설정 제거
}
