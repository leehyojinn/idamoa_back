package com.hip.damoa.infra.notification;

import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationChannel;
import com.hip.damoa.domain.notification.model.NotificationTemplate;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.service.NotificationLogService;
import com.hip.damoa.domain.notification.service.TemplateService;
import com.hip.damoa.domain.notification.service.TemplateService.RenderedTemplate;
import com.hip.damoa.infra.notification.provider.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public Long sendNotification(NotificationChannel channel, String recipient, String templateCode,
                                   Map<String, String> variables, Long userId, String notificationType) {
        log.info("UMS 발송 시작: channel={}, recipient={}, template={}", channel, recipient, templateCode);

        Notification notification = null;

        try {
            // 1. 템플릿 조회
            NotificationTemplate template = templateService.getTemplate(templateCode, channel);

            // 2. 템플릿 렌더링
            RenderedTemplate rendered = templateService.renderTemplate(template, variables);

            // 3. Notification 엔티티 생성
            notification = Notification.builder()
                    .userId(userId)
                    .notificationType(notificationType)
                    .channel(channel)
                    .recipientEmail(recipient)  // recipient 저장
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
                String errorMsg = "Provider not found for channel: " + channel;
                log.error("Provider를 찾을 수 없습니다: channel={}", channel);
                notification.markAsFailed(errorMsg);
                notificationRepository.save(notification);

                // 실패 로그 기록
                logFailure(notification, recipient, channel, "SYSTEM", "PROVIDER_NOT_FOUND", errorMsg);
                return notification.getId();
            }

            if (!provider.isEnabled()) {
                String errorMsg = "Provider disabled: " + channel;
                log.warn("Provider가 비활성화되어 있습니다: channel={}", channel);
                notification.markAsFailed(errorMsg);
                notificationRepository.save(notification);

                // 실패 로그 기록
                logFailure(notification, recipient, channel, provider.getClass().getSimpleName(),
                          "PROVIDER_DISABLED", errorMsg);
                return notification.getId();
            }

            String providerId = provider.send(recipient, rendered.title(), rendered.content(), variables);

            // 5. 발송 성공 처리
            notification.markAsSent();
            notificationRepository.save(notification);

            // 성공 로그 기록 (채널별)
            logSuccess(notification, recipient, channel, provider.getClass().getSimpleName(), providerId);

            log.info("UMS 발송 완료: channel={}, recipient={}, providerId={}", channel, recipient, providerId);
            return notification.getId();

        } catch (Exception e) {
            // 상세한 에러 메시지 추출 (root cause까지)
            String detailedError = extractDetailedErrorMessage(e);
            log.error("UMS 발송 실패: channel={}, recipient={}, error={}", channel, recipient, detailedError, e);

            // 예외 발생 시 Notification 실패 처리 및 로그 기록
            if (notification != null) {
                try {
                    notification.markAsFailed(detailedError);
                    notificationRepository.save(notification);

                    // 실패 로그 기록
                    logFailure(notification, recipient, channel, "UNKNOWN",
                              "SEND_FAILED", detailedError);
                } catch (Exception logEx) {
                    log.error("실패 로그 기록 중 오류 발생", logEx);
                }
            }

            throw new RuntimeException("메시지 발송 실패: " + detailedError, e);
        }
    }

    /**
     * 이메일 발송 (템플릿 사용)
     */
    public Long sendEmail(String email, String templateCode, Map<String, String> variables,
                          Long userId, String notificationType) {
        return sendNotification(NotificationChannel.EMAIL, email, templateCode, variables, userId, notificationType);
    }

    /**
     * SMS 발송 (템플릿 사용)
     */
    public Long sendSms(String phoneNumber, String templateCode, Map<String, String> variables,
                        Long userId, String notificationType) {
        return sendNotification(NotificationChannel.SMS, phoneNumber, templateCode, variables, userId, notificationType);
    }

    /**
     * 카카오 알림톡 발송 (템플릿 사용)
     */
    public Long sendKakao(String phoneNumber, String templateCode, Map<String, String> variables,
                          Long userId, String notificationType) {
        return sendNotification(NotificationChannel.KAKAO, phoneNumber, templateCode, variables, userId, notificationType);
    }

    /**
     * FCM 푸시 발송 (템플릿 사용)
     */
    public Long sendPush(String fcmToken, String templateCode, Map<String, String> variables,
                         Long userId, String notificationType) {
        return sendNotification(NotificationChannel.FCM, fcmToken, templateCode, variables, userId, notificationType);
    }

    /**
     * 채널에 맞는 Provider 찾기
     */
    private NotificationProvider findProvider(NotificationChannel channel) {
        return providers.stream()
                .filter(p -> p.getChannelType() == channel)
                .findFirst()
                .orElse(null);
    }

    /**
     * 발송 성공 로그 기록 (채널별)
     */
    private void logSuccess(Notification notification, String recipient, NotificationChannel channel,
                           String provider, String providerId) {
        switch (channel) {
            case EMAIL:
                notificationLogService.logEmailSent(notification, recipient, provider, providerId, null);
                break;
            case SMS:
                notificationLogService.logSmsSent(notification, recipient, provider, providerId, null, null);
                break;
            case KAKAO:
                // TODO: 카카오 전용 로그 메서드 추가 필요
                notificationLogService.logSmsSent(notification, recipient, provider, providerId, null, null);
                break;
            case FCM:
                // TODO: FCM 전용 로그 메서드 추가 필요
                notificationLogService.logEmailSent(notification, recipient, provider, providerId, null);
                break;
        }
    }

    /**
     * 발송 실패 로그 기록 (채널별)
     */
    private void logFailure(Notification notification, String recipient, NotificationChannel channel,
                           String provider, String errorCode, String errorMessage) {
        switch (channel) {
            case EMAIL:
                notificationLogService.logEmailFailed(notification, recipient, provider, errorCode, errorMessage);
                break;
            case SMS:
            case KAKAO:
                notificationLogService.logSmsFailed(notification, recipient, provider, errorCode, errorMessage);
                break;
            case FCM:
                // TODO: FCM 전용 로그 메서드 추가 필요
                notificationLogService.logEmailFailed(notification, recipient, provider, errorCode, errorMessage);
                break;
        }
    }

    /**
     * 예외에서 상세한 에러 메시지 추출 (root cause까지 탐색)
     */
    private String extractDetailedErrorMessage(Exception e) {
        // Root cause까지 탐색하여 가장 구체적인 에러 메시지 반환
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }

        // Root cause에 메시지가 없으면 원래 예외의 메시지 반환
        return e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
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
