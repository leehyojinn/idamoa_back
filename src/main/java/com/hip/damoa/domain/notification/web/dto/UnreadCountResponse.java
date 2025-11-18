package com.hip.damoa.domain.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미읽음 알림 개수 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "미읽음 알림 개수 응답")
public class UnreadCountResponse {

    @Schema(description = "미읽음 알림 개수", example = "3")
    private Long unreadCount;
}
