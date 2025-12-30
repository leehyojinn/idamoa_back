package com.hip.damoa.domain.directchat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * 타이핑 인디케이터 DTO (WebSocket용)
 */
@Schema(description = "타이핑 인디케이터")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicator {

    @Schema(description = "채팅방 UUID")
    private UUID roomUuid;

    @Schema(description = "사용자 UUID")
    private UUID userUuid;

    @Schema(description = "사용자 닉네임")
    private String nickname;

    @Schema(description = "타이핑 중 여부")
    private Boolean isTyping;
}
