package com.hip.damoa.domain.chat.web.dto;

import com.hip.damoa.domain.chat.model.ChatMessage;
import com.hip.damoa.domain.chat.model.SenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

    @Schema(description = "메시지 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID chatRoomUuid;

    @Schema(description = "발신자 타입 (USER or COMPANY)", example = "USER")
    private SenderType senderType;

    @Schema(description = "발신자 ID", example = "1")
    private Long senderId;

    @Schema(description = "메시지 내용", example = "안녕하세요, 견적에 대해 문의드립니다.")
    private String message;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;

    @Schema(description = "읽은 시각", example = "2025-11-18T10:00:00")
    private LocalDateTime readAt;

    @Schema(description = "생성 시각", example = "2025-11-18T09:30:00")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .uuid(chatMessage.getUuid())
                .chatRoomUuid(chatMessage.getChatRoom().getUuid())
                .senderType(chatMessage.getSenderType())
                .senderId(chatMessage.getSenderId())
                .message(chatMessage.getMessage())
                .isRead(chatMessage.getIsRead())
                .readAt(chatMessage.getReadAt())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
