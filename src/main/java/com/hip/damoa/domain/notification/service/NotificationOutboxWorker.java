package com.hip.damoa.domain.notification.service;

import com.hip.damoa.domain.notification.model.NotificationChannel;
import com.hip.damoa.domain.notification.model.NotificationOutbox;
import com.hip.damoa.domain.notification.model.OutboxStatus;
import com.hip.damoa.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Outbox 워커
 *
 * - 주기적으로 PENDING 상태의 알림을 조회하여 발송
 * - 채널별 Provider를 통해 실제 발송
 * - 실패 시 지수 백오프로 재시도
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxRepository outboxRepository;
    // TODO: 각 채널별 알림 Provider 추가
    // private final KakaoAlimtalkService kakaoService;
    // private final SmsService smsService;
    // private final EmailService emailService;
    // private final PushNotificationService pushService;

    private static final int BATCH_SIZE = 100;

    /**
     * Outbox 처리 스케줄러 (10초마다 실행)
     * - FOR UPDATE SKIP LOCKED로 다중 인스턴스 동시성 제어
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processOutbox() {
        // FOR UPDATE SKIP LOCKED으로 중복 처리 방지
        List<NotificationOutbox> pendingNotifications = outboxRepository.findAndLockPendingNotifications(
                LocalDateTime.now(),
                BATCH_SIZE
        );

        if (pendingNotifications.isEmpty()) {
            return;
        }

        log.debug("대기 중인 알림 {} 건 처리 시작", pendingNotifications.size());

        int processed = 0;

        for (NotificationOutbox outbox : pendingNotifications) {
            try {
                processNotificationInternal(outbox);
                processed++;
            } catch (Exception e) {
                log.error("알림 처리 실패: outboxId={}", outbox.getId(), e);
                // 개별 실패 시에도 다른 알림 처리 계속
                handleProcessingError(outbox, e.getMessage());
            }
        }

        log.info("알림 처리 완료: {} / {} 건", processed, pendingNotifications.size());
    }

    /**
     * 개별 알림 처리 (내부 메서드)
     * - 트랜잭션 내에서 상태 변경 후 외부 API 호출
     */
    private void processNotificationInternal(NotificationOutbox outbox) {
        log.debug("알림 처리: outboxId={}, channel={}, type={}",
                outbox.getId(), outbox.getChannel(), outbox.getNotificationType());

        // 1. 상태를 PROCESSING으로 변경 (트랜잭션 내)
        outbox.startProcessing();
        outboxRepository.saveAndFlush(outbox);

        // 2. 외부 API 호출 (실패해도 상태는 이미 저장됨)
        boolean success = false;
        try {
            success = sendNotification(outbox);
        } catch (Exception e) {
            log.error("알림 발송 오류: outboxId={}", outbox.getId(), e);
            outbox.markFailed(e.getMessage());
            outboxRepository.save(outbox);
            return;
        }

        // 3. 결과에 따라 상태 업데이트
        if (success) {
            outbox.markSent();
            log.info("알림 발송 성공: outboxId={}", outbox.getId());
        } else {
            outbox.markFailed("발송 실패");
            log.warn("알림 발송 실패: outboxId={}, retryCount={}", outbox.getId(), outbox.getRetryCount());
        }

        outboxRepository.save(outbox);
    }

    /**
     * 처리 오류 핸들링
     */
    private void handleProcessingError(NotificationOutbox outbox, String errorMessage) {
        try {
            outbox.markFailed(errorMessage);
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("알림 오류 처리 실패: outboxId={}", outbox.getId(), e);
        }
    }

    /**
     * 채널별 알림 발송
     */
    private boolean sendNotification(NotificationOutbox outbox) {
        NotificationChannel channel = outbox.getChannel();

        switch (channel) {
            case KAKAO:
                return sendKakaoAlimtalk(outbox);
            case SMS:
                return sendSms(outbox);
            case EMAIL:
                return sendEmail(outbox);
            case FCM:
                return sendPush(outbox);
            default:
                log.warn("지원하지 않는 알림 채널: {}", channel);
                return false;
        }
    }

    /**
     * 카카오 알림톡 발송
     */
    private boolean sendKakaoAlimtalk(NotificationOutbox outbox) {
        // TODO: 카카오 알림톡 서비스 연동
        // return kakaoService.send(
        //     outbox.getRecipientPhone(),
        //     outbox.getTemplateCode(),
        //     outbox.getTemplateData()
        // );

        log.info("[Mock] 카카오 알림톡 발송: recipientId={}, content={}",
                outbox.getRecipient().getId(),
                outbox.getContent().substring(0, Math.min(50, outbox.getContent().length())));

        // 임시로 성공 반환 (실제 연동 시 수정)
        return true;
    }

    /**
     * SMS 발송
     */
    private boolean sendSms(NotificationOutbox outbox) {
        // TODO: SMS 서비스 연동
        log.info("[Mock] SMS 발송: recipientId={}", outbox.getRecipient().getId());
        return true;
    }

    /**
     * 이메일 발송
     */
    private boolean sendEmail(NotificationOutbox outbox) {
        // TODO: 이메일 서비스 연동
        log.info("[Mock] 이메일 발송: recipientId={}", outbox.getRecipient().getId());
        return true;
    }

    /**
     * 푸시 알림 발송
     */
    private boolean sendPush(NotificationOutbox outbox) {
        // TODO: FCM 푸시 서비스 연동
        log.info("[Mock] 푸시 알림 발송: recipientId={}", outbox.getRecipient().getId());
        return true;
    }

    /**
     * 오래된 처리 완료 알림 정리 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = outboxRepository.deleteOldProcessedNotifications(threshold);
        if (deleted > 0) {
            log.info("오래된 알림 {} 건 삭제됨", deleted);
        }
    }
}
