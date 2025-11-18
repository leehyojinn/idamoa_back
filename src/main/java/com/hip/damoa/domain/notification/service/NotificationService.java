package com.hip.damoa.domain.notification.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationChannel;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.web.dto.NotificationMessage;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 통합 알림 서비스
 *
 * DB 저장 + WebSocket 실시간 전송을 함께 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketService;

    /**
     * 견적 제안 도착 알림
     *
     * @param userEmail 견적 요청 작성자 이메일
     * @param estimateRequestUuid 견적 요청 UUID
     * @param companyName 제안 업체명
     * @param proposalTitle 제안 제목
     */
    @Transactional
    public void notifyNewProposal(
            String userEmail,
            UUID estimateRequestUuid,
            String companyName,
            String proposalTitle) {

        log.info("견적 제안 알림 발송: userEmail={}, estimateRequestUuid={}, companyName={}",
                userEmail, estimateRequestUuid, companyName);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. DB에 Notification 저장
        Notification notification = Notification.builder()
                .userId(user.getId())
                .recipient(user)
                .recipientEmail(userEmail)
                .notificationType("NEW_PROPOSAL")
                .channel(NotificationChannel.FCM)  // 앱 내 알림 (WebSocket + FCM)
                .title("새로운 제안이 도착했습니다")
                .content(String.format("%s에서 '%s' 제안을 제출했습니다", companyName, proposalTitle))
                .templateData(Map.of(
                    "estimateRequestUuid", estimateRequestUuid.toString(),
                    "companyName", companyName,
                    "proposalTitle", proposalTitle
                ))
                .isSent(false)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // 2. WebSocket으로 실시간 전송
        NotificationMessage message = NotificationMessage.newProposal(
                estimateRequestUuid,
                companyName,
                proposalTitle
        );

        webSocketService.sendToUser(userEmail, message);

        log.info("견적 제안 알림 발송 완료: notificationId={}", notification.getId());
    }

    /**
     * 상담 답변 도착 알림
     *
     * @param userEmail 상담 신청자 이메일
     * @param consultationUuid 상담 UUID
     * @param responseMessage 답변 내용
     */
    @Transactional
    public void notifyConsultationResponse(
            String userEmail,
            UUID consultationUuid,
            String responseMessage) {

        log.info("상담 답변 알림 발송: userEmail={}, consultationUuid={}",
                userEmail, consultationUuid);

        // 사용자 조회 (상담은 비회원도 가능하므로 optional)
        User user = userRepository.findByEmail(userEmail).orElse(null);

        // 응답 미리보기 (100자 제한)
        String responsePreview = responseMessage.length() > 100
                ? responseMessage.substring(0, 100) + "..."
                : responseMessage;

        // 1. DB에 Notification 저장
        Notification notification = Notification.builder()
                .userId(user != null ? user.getId() : null)
                .recipient(user)
                .recipientEmail(userEmail)
                .notificationType("CONSULTATION_RESPONSE")
                .channel(NotificationChannel.FCM)  // 앱 내 알림 (WebSocket + FCM)
                .title("상담 답변이 도착했습니다")
                .content(responsePreview)
                .templateData(Map.of(
                    "consultationUuid", consultationUuid.toString(),
                    "responseMessage", responseMessage
                ))
                .isSent(false)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // 2. WebSocket으로 실시간 전송 (회원만)
        if (user != null) {
            NotificationMessage message = NotificationMessage.consultationResponse(
                    consultationUuid,
                    responsePreview
            );

            webSocketService.sendToUser(userEmail, message);
        }

        log.info("상담 답변 알림 발송 완료: notificationId={}", notification.getId());
    }
}
