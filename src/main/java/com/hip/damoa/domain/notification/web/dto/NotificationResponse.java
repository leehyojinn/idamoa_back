package com.hip.damoa.domain.notification.web.dto;

import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.notification.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 알림 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 응답")
public class NotificationResponse {

    @Schema(description = "알림 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "알림 타입",
            example = "NEW_PROPOSAL",
            allowableValues = {"NEW_PROPOSAL", "CONSULTATION_RESPONSE", "PROPOSAL_SELECTED", "PAYMENT_COMPLETED"})
    private String notificationType;

    @Schema(description = "알림 채널", example = "FCM")
    private NotificationChannel channel;

    @Schema(description = "제목", example = "새로운 제안이 도착했습니다")
    private String title;

    @Schema(description = "내용", example = "ABC 인테리어에서 제안을 제출했습니다")
    private String content;

    @Schema(description = "템플릿 데이터",
            example = "{\"estimateRequestUuid\": \"550e8400-e29b-41d4-a716-446655440000\", \"companyName\": \"ABC 인테리어\"}")
    private Map<String, String> templateData;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "읽은 시각", example = "2025-11-18T10:00:00")
    private LocalDateTime readAt;

    @Schema(description = "생성 시각", example = "2025-11-18T09:30:00")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .uuid(notification.getUuid())
                .notificationType(notification.getNotificationType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .content(notification.getContent())
                .templateData(notification.getTemplateData())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
