package com.hip.damoa.infra.notification;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Gmail API 기반 이메일 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.email.gmail-api.enabled", havingValue = "true")
public class GmailService {

    private final Gmail gmail;

    @Value("${notification.email.from}")
    private String from;

    /**
     * HTML 이메일 발송
     *
     * @param to      수신자 이메일
     * @param subject 제목
     * @param html    HTML 본문
     * @return Gmail Message ID
     */
    public String sendHtmlEmail(String to, String subject, String html) {
        try {
            log.info("Gmail API 이메일 발송 시작: to={}, subject={}", to, subject);

            MimeMessage mimeMessage = buildHtmlMessage(from, to, subject, html);
            Message rawMessage = toRawMessage(mimeMessage);
            Message sent = gmail.users().messages().send("me", rawMessage).execute();

            log.info("Gmail API 이메일 발송 완료: to={}, messageId={}", to, sent.getId());
            return sent.getId();
        } catch (GoogleJsonResponseException e) {
            // Gmail API 에러 메시지 파싱 (핵심 메시지만 추출)
            String errorMessage = extractGmailErrorMessage(e);
            log.error("Gmail API 이메일 발송 실패: to={}, subject={}, error={}", to, subject, errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            log.error("Gmail API 이메일 발송 실패: to={}, subject={}", to, subject, e);
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 텍스트 이메일 발송
     *
     * @param to      수신자 이메일
     * @param subject 제목
     * @param text    텍스트 본문
     * @return Gmail Message ID
     */
    public String sendTextEmail(String to, String subject, String text) {
        try {
            log.info("Gmail API 텍스트 이메일 발송 시작: to={}, subject={}", to, subject);

            MimeMessage mimeMessage = buildTextMessage(from, to, subject, text);
            Message rawMessage = toRawMessage(mimeMessage);
            Message sent = gmail.users().messages().send("me", rawMessage).execute();

            log.info("Gmail API 텍스트 이메일 발송 완료: to={}, messageId={}", to, sent.getId());
            return sent.getId();
        } catch (GoogleJsonResponseException e) {
            // Gmail API 에러 메시지 파싱 (핵심 메시지만 추출)
            String errorMessage = extractGmailErrorMessage(e);
            log.error("Gmail API 텍스트 이메일 발송 실패: to={}, subject={}, error={}", to, subject, errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            log.error("Gmail API 텍스트 이메일 발송 실패: to={}, subject={}", to, subject, e);
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * HTML MIME 메시지 생성
     */
    private MimeMessage buildHtmlMessage(String from, String to, String subject, String html) throws Exception {
        Properties props = new Properties();
        Session session = Session.getInstance(props);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to, false));
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        email.setContent(html, "text/html; charset=UTF-8");
        return email;
    }

    /**
     * 텍스트 MIME 메시지 생성
     */
    private MimeMessage buildTextMessage(String from, String to, String subject, String text) throws Exception {
        Properties props = new Properties();
        Session session = Session.getInstance(props);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to, false));
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        email.setText(text, StandardCharsets.UTF_8.name());
        return email;
    }

    /**
     * MIME 메시지를 Gmail API Message로 변환
     */
    private Message toRawMessage(MimeMessage mimeMessage) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        return new Message().setRaw(encoded);
    }

    /**
     * Gmail API 에러 메시지에서 핵심 메시지만 추출
     *
     * 예: "400 Bad Request\n{...JSON...}" → "Invalid To header"
     */
    private String extractGmailErrorMessage(GoogleJsonResponseException e) {
        try {
            // Gmail API 에러 구조: { "error": { "errors": [ { "message": "..." } ], "message": "..." } }
            if (e.getDetails() != null && e.getDetails().getMessage() != null) {
                return "Gmail API 오류: " + e.getDetails().getMessage();
            }

            // 상세 에러가 없으면 HTTP 상태 코드와 기본 메시지 사용
            return String.format("Gmail API 오류 (HTTP %d): %s",
                    e.getStatusCode(),
                    e.getStatusMessage() != null ? e.getStatusMessage() : "알 수 없는 오류");
        } catch (Exception ex) {
            // 파싱 실패 시 원본 메시지 반환
            return "Gmail API 오류: " + e.getMessage();
        }
    }
}
