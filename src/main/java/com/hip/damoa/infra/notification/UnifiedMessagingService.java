package com.hip.damoa.infra.notification;

import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationTemplate;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.service.NotificationLogService;
import com.hip.damoa.domain.notification.service.TemplateService;
import com.hip.damoa.domain.notification.service.TemplateService.RenderedTemplate;
import com.hip.damoa.infra.notification.provider.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * UMS (Unified Messaging Service) - 통합 메시징 서비스
 *
 * 지원 채널:
 * - EMAIL: Gmail API
 * - SMS: ALIGO, NCP
 * - KAKAO: 카카오 알림톡
 * - FCM: Firebase Cloud Messaging
 *
 * 특징:
 * - 템플릿 기반 메시지 발송
 * - 다중 채널 지원
 * - 발송 로그 자동 기록
 * - 비동기 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedMessagingService {

    private final List<NotificationProvider> providers;
    private final TemplateService templateService;
    private final NotificationRepository notificationRepository;
    private final NotificationLogService notificationLogService;

    /**
     * 템플릿 기반 메시지 발송
     *
     * @param channel        발송 채널 (EMAIL, SMS, KAKAO, FCM)
     * @param recipient      수신자
     * @param templateCode   템플릿 코드
     * @param variables      템플릿 변수
     * @param userId         사용자 ID (null 가능)
     * @param notificationType 알림 타입
     * @return Notification ID
     */
    @Async
    public Long sendNotification(String channel, String recipient, String templateCode,
                                   Map<String, String> variables, Long userId, String notificationType) {
        log.info("UMS 발송 시작: channel={}, recipient={}, template={}", channel, recipient, templateCode);

        try {
            // 1. 템플릿 조회
            NotificationTemplate template = templateService.getTemplate(templateCode, channel);

            // 2. 템플릿 렌더링
            RenderedTemplate rendered = templateService.renderTemplate(template, variables);

            // 3. Notification 엔티티 생성
            Notification notification = Notification.builder()
                    .userId(userId)
                    .notificationType(notificationType)
                    .channel(channel)
                    .title(rendered.title())
                    .content(rendered.content())
                    .templateId(template.getId())
                    .templateData(variables)
                    .isSent(false)
                    .build();
            notification = notificationRepository.save(notification);

            // 4. Provider 선택 및 발송
            NotificationProvider provider = findProvider(channel);
            if (provider == null) {
                log.error("Provider를 찾을 수 없습니다: channel={}", channel);
                notification.markAsFailed("Provider not found for channel: " + channel);
                notificationRepository.save(notification);
                return notification.getId();
            }

            if (!provider.isEnabled()) {
                log.warn("Provider가 비활성화되어 있습니다: channel={}", channel);
                notification.markAsFailed("Provider disabled: " + channel);
                notificationRepository.save(notification);
                return notification.getId();
            }

            String providerId = provider.send(recipient, rendered.title(), rendered.content(), variables);

            // 5. 발송 성공 처리
            notification.markAsSent();
            notificationRepository.save(notification);
            notificationLogService.logEmailSent(notification, recipient, provider.getClass().getSimpleName(),
                    providerId, null);

            log.info("UMS 발송 완료: channel={}, recipient={}, providerId={}", channel, recipient, providerId);
            return notification.getId();

        } catch (Exception e) {
            log.error("UMS 발송 실패: channel={}, recipient={}, error={}", channel, recipient, e.getMessage(), e);
            throw new RuntimeException("메시지 발송 실패", e);
        }
    }

    /**
     * 이메일 발송 (템플릿 사용)
     */
    @Async
    public Long sendEmail(String email, String templateCode, Map<String, String> variables,
                          Long userId, String notificationType) {
        return sendNotification("EMAIL", email, templateCode, variables, userId, notificationType);
    }

    /**
     * SMS 발송 (템플릿 사용)
     */
    @Async
    public Long sendSms(String phoneNumber, String templateCode, Map<String, String> variables,
                        Long userId, String notificationType) {
        return sendNotification("SMS", phoneNumber, templateCode, variables, userId, notificationType);
    }

    /**
     * 카카오 알림톡 발송 (템플릿 사용)
     */
    @Async
    public Long sendKakao(String phoneNumber, String templateCode, Map<String, String> variables,
                          Long userId, String notificationType) {
        return sendNotification("KAKAO", phoneNumber, templateCode, variables, userId, notificationType);
    }

    /**
     * FCM 푸시 발송 (템플릿 사용)
     */
    @Async
    public Long sendPush(String fcmToken, String templateCode, Map<String, String> variables,
                         Long userId, String notificationType) {
        return sendNotification("FCM", fcmToken, templateCode, variables, userId, notificationType);
    }

    /**
     * 채널에 맞는 Provider 찾기
     */
    private NotificationProvider findProvider(String channel) {
        return providers.stream()
                .filter(p -> p.getChannelType().equalsIgnoreCase(channel))
                .findFirst()
                .orElse(null);
    }

    /**
     * 새 제안 도착 알림 (이메일)
     */
    public void notifyNewProposal(String userEmail, Long userId, String estimateTitle, int proposalCount) {
        Map<String, String> variables = Map.of(
                "estimateTitle", estimateTitle,
                "proposalCount", String.valueOf(proposalCount)
        );
        // TODO: 템플릿 생성 후 사용
        // sendEmail(userEmail, "NEW_PROPOSAL", variables, userId, "NEW_PROPOSAL");
        log.info("새 제안 도착 알림 (템플릿 없음): email={}, estimateTitle={}", userEmail, estimateTitle);
    }

    /**
     * 콘테스트 우승자 선정 알림 (이메일)
     */
    public void notifyContestWinner(String userEmail, Long userId, String contestTitle, String prizeName) {
        Map<String, String> variables = Map.of(
                "contestTitle", contestTitle,
                "prizeName", prizeName
        );
        // TODO: 템플릿 생성 후 사용
        // sendEmail(userEmail, "CONTEST_WINNER", variables, userId, "CONTEST_WINNER");
        log.info("콘테스트 우승자 알림 (템플릿 없음): email={}, contestTitle={}", userEmail, contestTitle);
    }

    /**
     * 결제 완료 알림 (이메일)
     */
    public void notifyPaymentCompleted(String userEmail, Long userId, String orderName, String amount) {
        Map<String, String> variables = Map.of(
                "orderName", orderName,
                "amount", amount
        );
        // TODO: 템플릿 생성 후 사용
        // sendEmail(userEmail, "PAYMENT_COMPLETED", variables, userId, "PAYMENT_COMPLETED");
        log.info("결제 완료 알림 (템플릿 없음): email={}, orderName={}", userEmail, orderName);
    }

    /**
     * 구독 만료 임박 알림 (이메일)
     */
    public void notifySubscriptionExpiring(String userEmail, Long userId, String planName, int daysLeft) {
        Map<String, String> variables = Map.of(
                "planName", planName,
                "daysLeft", String.valueOf(daysLeft)
        );
        // TODO: 템플릿 생성 후 사용
        // sendEmail(userEmail, "SUBSCRIPTION_EXPIRING", variables, userId, "SUBSCRIPTION_EXPIRING");
        log.info("구독 만료 임박 알림 (템플릿 없음): email={}, planName={}, daysLeft={}", userEmail, planName, daysLeft);
    }
}
