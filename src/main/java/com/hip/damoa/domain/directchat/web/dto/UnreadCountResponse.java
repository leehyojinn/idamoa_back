package com.hip.damoa.domain.directchat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 미읽음 메시지 수 응답 DTO
 */
@Schema(description = "미읽음 메시지 수 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    @Schema(description = "미읽음 메시지 수", example = "5")
    private Long count;

    public static UnreadCountResponse of(long count) {
        return UnreadCountResponse.builder()
                .count(count)
                .build();
    }
}
