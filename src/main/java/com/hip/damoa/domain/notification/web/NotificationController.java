package com.hip.damoa.domain.notification.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.repository.NotificationRepository;
import com.hip.damoa.domain.notification.web.dto.NotificationListResponse;
import com.hip.damoa.domain.notification.web.dto.NotificationResponse;
import com.hip.damoa.domain.notification.web.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 알림 REST API 컨트롤러
 *
 * WebSocket 대신 HTTP로 알림 관리
 * - 알림 목록 조회
 * - 미읽음 개수 조회
 * - 알림 읽음 처리
 * - 알림 삭제
 */
@Tag(name = "Notification", description = "알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 목록 조회
     *
     * @param userDetails 인증된 사용자
     * @return 알림 목록
     */
    @Operation(
            summary = "알림 목록 조회",
            description = "현재 사용자의 모든 알림을 최신순으로 조회합니다 (삭제되지 않은 알림만)"
    )

    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("알림 목록 조회: userEmail={}", userEmail);

        List<Notification> notifications = notificationRepository
                .findByRecipientEmailAndIsDeletedFalseOrderByCreatedAtDesc(userEmail);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();

        NotificationListResponse response = NotificationListResponse.builder()
                .notifications(responses)
                .totalCount(responses.size())
                .build();

        log.info("알림 목록 조회 완료: userEmail={}, count={}", userEmail, responses.size());
        return ApiResponse.success(response);
    }

    /**
     * 미읽음 알림 개수 조회
     *
     * @param userDetails 인증된 사용자
     * @return 미읽음 알림 개수
     */
    @Operation(
            summary = "미읽음 알림 개수 조회",
            description = "현재 사용자의 미읽음 알림 개수를 조회합니다"
    )
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("미읽음 알림 개수 조회: userEmail={}", userEmail);

        long unreadCount = notificationRepository
                .countByRecipientEmailAndIsReadFalseAndIsDeletedFalse(userEmail);

        UnreadCountResponse response = UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();

        log.info("미읽음 알림 개수 조회 완료: userEmail={}, count={}", userEmail, unreadCount);
        return ApiResponse.success(response);
    }

    /**
     * 알림 읽음 처리
     *
     * @param uuid 알림 UUID
     * @param userDetails 인증된 사용자
     * @return 읽음 처리된 알림
     */
    @Operation(
            summary = "알림 읽음 처리",
            description = "특정 알림을 읽음 상태로 변경합니다"
    )
    @PatchMapping("/{uuid}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("알림 읽음 처리: uuid={}, userEmail={}", uuid, userEmail);

        Notification notification = notificationRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getRecipientEmail().equals(userEmail)) {
            log.warn("알림 접근 권한 없음: uuid={}, userEmail={}, recipientEmail={}",
                    uuid, userEmail, notification.getRecipientEmail());
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 읽음 처리
        notification.markAsRead();
        notificationRepository.save(notification);

        log.info("알림 읽음 처리 완료: uuid={}", uuid);
        return ApiResponse.success(NotificationResponse.from(notification));
    }

    /**
     * 알림 삭제 (Soft Delete)
     *
     * @param uuid 알림 UUID
     * @param userDetails 인증된 사용자
     * @return 성공 응답
     */
    @Operation(
            summary = "알림 삭제",
            description = "특정 알림을 삭제합니다 (소프트 삭제)"
    )
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteNotification(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("알림 삭제: uuid={}, userEmail={}", uuid, userEmail);

        Notification notification = notificationRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getRecipientEmail().equals(userEmail)) {
            log.warn("알림 접근 권한 없음: uuid={}, userEmail={}, recipientEmail={}",
                    uuid, userEmail, notification.getRecipientEmail());
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // Soft delete
        notification.softDelete();
        notificationRepository.save(notification);

        log.info("알림 삭제 완료: uuid={}", uuid);
        return ApiResponse.success();
    }

    /**
     * 특정 알림 조회
     *
     * @param uuid 알림 UUID
     * @param userDetails 인증된 사용자
     * @return 알림 상세 정보
     */
    @Operation(
            summary = "알림 상세 조회",
            description = "특정 알림의 상세 정보를 조회합니다"
    )
    @GetMapping("/{uuid}")
    public ApiResponse<NotificationResponse> getNotification(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        log.info("알림 상세 조회: uuid={}, userEmail={}", uuid, userEmail);

        Notification notification = notificationRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getRecipientEmail().equals(userEmail)) {
            log.warn("알림 접근 권한 없음: uuid={}, userEmail={}, recipientEmail={}",
                    uuid, userEmail, notification.getRecipientEmail());
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        log.info("알림 상세 조회 완료: uuid={}", uuid);
        return ApiResponse.success(NotificationResponse.from(notification));
    }
}
