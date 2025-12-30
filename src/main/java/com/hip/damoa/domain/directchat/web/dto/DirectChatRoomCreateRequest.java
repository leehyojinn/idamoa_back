package com.hip.damoa.domain.directchat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * 채팅방 생성 요청 DTO
 */
@Schema(description = "채팅방 생성 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatRoomCreateRequest {

    @Schema(description = "대상 사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "대상 사용자 UUID는 필수입니다")
    private UUID targetUserUuid;
}
