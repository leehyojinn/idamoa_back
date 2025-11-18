package com.hip.damoa.domain.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 알림 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 목록 응답")
public class NotificationListResponse {

    @Schema(description = "알림 목록")
    private List<NotificationResponse> notifications;

    @Schema(description = "전체 알림 개수", example = "15")
    private Integer totalCount;
}
