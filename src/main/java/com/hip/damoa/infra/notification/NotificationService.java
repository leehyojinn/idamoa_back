package com.hip.damoa.infra.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Notification Service
 *
 * 이메일/SMS 알림 전송
 *
 * 주요 알림 시나리오:
 * 1. 새 제안 도착 (견적 요청자에게)
 * 2. 콘테스트 우승자 선정 (참가자에게)
 * 3. 결제 완료 (결제자에게)
 * 4. 구독 만료 임박 (구독자에게)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String emailFrom;

    @Value("${notification.sms.enabled}")
    private boolean smsEnabled;

    /**
     * 이메일 전송 (비동기)
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email notification disabled - skipped: to={}, subject={}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email: to={}, subject={}", to, subject, e);
        }
    }

    /**
     * SMS 전송 (비동기)
     *
     * 실제 구현 시 ALIGO 또는 NCP SMS API 연동 필요
     */
    @Async
    public void sendSMS(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS notification disabled - skipped: to={}, message={}", phoneNumber, message);
            return;
        }

        // 실제 SMS 전송 구현 필요
        // ALIGO: https://smartsms.aligo.in/admin/api/spec.html
        // NCP: https://api.ncloud-docs.com/docs/ai-application-service-sens-smsv2

        log.info("SMS send (mock): to={}, message={}", phoneNumber, message);
    }

    /**
     * 새 제안 도착 알림
     */
    public void notifyNewProposal(String userEmail, String estimateTitle, int proposalCount) {
        String subject = "[다모아] 새로운 견적 제안이 도착했습니다";
        String body = String.format(
            "안녕하세요,\n\n" +
            "회원님의 견적 요청 '%s'에 새로운 제안이 도착했습니다.\n\n" +
            "현재까지 도착한 제안: %d개\n\n" +
            "지금 바로 확인하시고 최적의 파트너를 선택하세요!\n\n" +
            "감사합니다.\n" +
            "다모아 드림",
            estimateTitle, proposalCount
        );

        sendEmail(userEmail, subject, body);
    }

    /**
     * 콘테스트 우승자 선정 알림
     */
    public void notifyContestWinner(String userEmail, String contestTitle, String prizeName) {
        String subject = "[다모아] 축하합니다! 콘테스트 우승자로 선정되셨습니다";
        String body = String.format(
            "축하합니다!\n\n" +
            "회원님께서 '%s' 콘테스트에서 우승하셨습니다.\n\n" +
            "상금: %s\n\n" +
            "상금은 영업일 기준 3-5일 이내에 등록하신 계좌로 입금됩니다.\n\n" +
            "앞으로도 멋진 작품 부탁드립니다!\n\n" +
            "감사합니다.\n" +
            "다모아 드림",
            contestTitle, prizeName
        );

        sendEmail(userEmail, subject, body);
    }

    /**
     * 결제 완료 알림
     */
    public void notifyPaymentCompleted(String userEmail, String orderName, String amount) {
        String subject = "[다모아] 결제가 완료되었습니다";
        String body = String.format(
            "안녕하세요,\n\n" +
            "결제가 정상적으로 완료되었습니다.\n\n" +
            "주문 내역: %s\n" +
            "결제 금액: %s원\n\n" +
            "이용해 주셔서 감사합니다.\n\n" +
            "다모아 드림",
            orderName, amount
        );

        sendEmail(userEmail, subject, body);
    }

    /**
     * 구독 만료 임박 알림
     */
    public void notifySubscriptionExpiring(String userEmail, String planName, int daysLeft) {
        String subject = "[다모아] 구독이 곧 만료됩니다";
        String body = String.format(
            "안녕하세요,\n\n" +
            "회원님의 %s 구독이 %d일 후 만료됩니다.\n\n" +
            "계속 이용하시려면 구독을 갱신해 주세요.\n\n" +
            "감사합니다.\n" +
            "다모아 드림",
            planName, daysLeft
        );

        sendEmail(userEmail, subject, body);
    }
}
