package com.hip.damoa.domain.notification.service;

import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationChannel;
import com.hip.damoa.domain.notification.model.NotificationLog;
import com.hip.damoa.domain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 발송 로그 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository logRepository;

    /**
     * 이메일 발송 성공 로그 기록 (비동기)
     *
     * @param notification Notification 엔티티
     * @param recipient    수신자 이메일
     * @param provider     발송 제공자 (GMAIL_API, SMTP 등)
     * @param providerId   제공자 메시지 ID
     * @param responseData 응답 데이터
     */
    @Async
    @Transactional
    public void logEmailSent(Notification notification, String recipient, String provider,
                              String providerId, Map<String, Object> responseData) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .notification(notification)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipient)
                    .status("SENT")
                    .provider(provider)
                    .providerMessageId(providerId)
                    .responseData(responseData != null ? responseData : Map.of())
                    .sentAt(LocalDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.info("이메일 발송 로그 기록 완료: notificationId={}, recipient={}, providerId={}",
                    notification.getId(), recipient, providerId);
        } catch (Exception e) {
            log.error("이메일 발송 로그 기록 실패: notificationId={}, recipient={}",
                    notification.getId(), recipient, e);
        }
    }

    /**
     * 이메일 발송 실패 로그 기록 (비동기)
     *
     * @param notification Notification 엔티티
     * @param recipient    수신자 이메일
     * @param provider     발송 제공자
     * @param errorCode    에러 코드
     * @param errorMessage 에러 메시지
     */
    @Async
    @Transactional
    public void logEmailFailed(Notification notification, String recipient, String provider,
                                String errorCode, String errorMessage) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .notification(notification)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipient)
                    .status("FAILED")
                    .provider(provider)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .failedAt(LocalDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.error("이메일 발송 실패 로그 기록: notificationId={}, recipient={}, error={}",
                    notification.getId(), recipient, errorMessage);
        } catch (Exception e) {
            log.error("이메일 발송 실패 로그 기록 실패: notificationId={}, recipient={}",
                    notification.getId(), recipient, e);
        }
    }

    /**
     * SMS 발송 성공 로그 기록 (비동기)
     *
     * @param notification Notification 엔티티
     * @param recipient    수신자 전화번호
     * @param provider     발송 제공자 (ALIGO, NCP 등)
     * @param providerId   제공자 메시지 ID
     * @param cost         발송 비용
     * @param responseData 응답 데이터
     */
    @Async
    @Transactional
    public void logSmsSent(Notification notification, String recipient, String provider,
                            String providerId, Double cost, Map<String, Object> responseData) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .notification(notification)
                    .channel(NotificationChannel.SMS)
                    .recipient(recipient)
                    .status("SENT")
                    .provider(provider)
                    .providerMessageId(providerId)
                    .responseData(responseData != null ? responseData : Map.of())
                    .sentAt(LocalDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.info("SMS 발송 로그 기록 완료: notificationId={}, recipient={}, providerId={}",
                    notification.getId(), recipient, providerId);
        } catch (Exception e) {
            log.error("SMS 발송 로그 기록 실패: notificationId={}, recipient={}",
                    notification.getId(), recipient, e);
        }
    }

    /**
     * SMS 발송 실패 로그 기록 (비동기)
     *
     * @param notification Notification 엔티티
     * @param recipient    수신자 전화번호
     * @param provider     발송 제공자
     * @param errorCode    에러 코드
     * @param errorMessage 에러 메시지
     */
    @Async
    @Transactional
    public void logSmsFailed(Notification notification, String recipient, String provider,
                              String errorCode, String errorMessage) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .notification(notification)
                    .channel(NotificationChannel.SMS)
                    .recipient(recipient)
                    .status("FAILED")
                    .provider(provider)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .failedAt(LocalDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.error("SMS 발송 실패 로그 기록: notificationId={}, recipient={}, error={}",
                    notification.getId(), recipient, errorMessage);
        } catch (Exception e) {
            log.error("SMS 발송 실패 로그 기록 실패: notificationId={}, recipient={}",
                    notification.getId(), recipient, e);
        }
    }
}
