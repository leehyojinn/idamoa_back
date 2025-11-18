package com.hip.damoa.domain.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 알림 메시지 DTO
 *
 * 실시간 알림 전송 시 사용되는 메시지 포맷
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WebSocket 알림 메시지")
public class NotificationMessage {

    @Schema(description = "알림 타입",
            example = "NEW_PROPOSAL",
            allowableValues = {"NEW_PROPOSAL", "CONSULTATION_RESPONSE", "PROPOSAL_SELECTED", "PAYMENT_COMPLETED"})
    private String type;

    @Schema(description = "관련 UUID (견적 요청 UUID 또는 상담 UUID)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "알림 제목", example = "새로운 제안이 도착했습니다")
    private String title;

    @Schema(description = "알림 내용", example = "ABC 인테리어에서 제안을 제출했습니다")
    private String content;

    @Schema(description = "추가 데이터 (업체명, 제목 등)",
            example = "{\"companyName\": \"ABC 인테리어\", \"proposalTitle\": \"50평 치과 인테리어 견적\"}")
    private Map<String, Object> data;

    @Schema(description = "클릭 시 이동할 URL",
            example = "/estimates/requests/550e8400-e29b-41d4-a716-446655440000")
    private String actionUrl;

    @Schema(description = "알림 발생 시각", example = "2025-11-17T15:00:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 견적 제안 도착 알림 생성
     */
    public static NotificationMessage newProposal(
            UUID estimateRequestUuid,
            String companyName,
            String proposalTitle) {

        return NotificationMessage.builder()
                .type("NEW_PROPOSAL")
                .uuid(estimateRequestUuid)
                .title("새로운 제안이 도착했습니다")
                .content(String.format("%s에서 제안을 제출했습니다", companyName))
                .data(Map.of(
                    "companyName", companyName,
                    "proposalTitle", proposalTitle
                ))
                .actionUrl("/estimates/requests/" + estimateRequestUuid)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 상담 답변 도착 알림 생성
     */
    public static NotificationMessage consultationResponse(
            UUID consultationUuid,
            String responsePreview) {

        return NotificationMessage.builder()
                .type("CONSULTATION_RESPONSE")
                .uuid(consultationUuid)
                .title("상담 답변이 도착했습니다")
                .content(responsePreview)
                .data(Map.of(
                    "consultationUuid", consultationUuid.toString()
                ))
                .actionUrl("/consultations/" + consultationUuid)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
