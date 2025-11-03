package com.hip.damoa.config.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

/**
 * Email Configuration
 *
 * Spring Mail 설정
 *
 * Gmail 사용 시:
 * 1. Google 계정에서 2단계 인증 활성화
 * 2. 앱 비밀번호 생성: https://myaccount.google.com/apppasswords
 * 3. 환경변수 설정:
 *    - SMTP_USERNAME: Gmail 주소
 *    - SMTP_PASSWORD: 생성한 앱 비밀번호
 */
@Configuration
@EnableAsync
public class EmailConfig {

    @Value("${notification.email.smtp.host}")
    private String host;

    @Value("${notification.email.smtp.port}")
    private int port;

    @Value("${notification.email.smtp.username}")
    private String username;

    @Value("${notification.email.smtp.password}")
    private String password;

    @Value("${notification.email.smtp.auth}")
    private boolean auth;

    @Value("${notification.email.smtp.starttls}")
    private boolean starttls;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.debug", "false");

        return mailSender;
    }
}
