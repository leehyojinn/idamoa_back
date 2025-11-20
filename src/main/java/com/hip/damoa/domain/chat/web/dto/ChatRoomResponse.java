package com.hip.damoa.domain.chat.web.dto;

import com.hip.damoa.domain.chat.model.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅방 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 응답")
public class ChatRoomResponse {

    @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "견적 요청 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID estimateRequestUuid;

    @Schema(description = "견적 요청 제목", example = "50평 치과 인테리어 견적")
    private String estimateRequestTitle;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    @Schema(description = "업체 ID", example = "10")
    private Long companyId;

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "ABC 인테리어")
    private String companyName;

    @Schema(description = "마지막 메시지", example = "안녕하세요, 견적에 대해 문의드립니다.")
    private String lastMessage;

    @Schema(description = "마지막 메시지 시각", example = "2025-11-18T10:00:00")
    private LocalDateTime lastMessageAt;

    @Schema(description = "사용자 미읽음 개수", example = "3")
    private Integer unreadCountUser;

    @Schema(description = "업체 미읽음 개수", example = "0")
    private Integer unreadCountCompany;

    @Schema(description = "생성 시각", example = "2025-11-18T09:00:00")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static ChatRoomResponse from(ChatRoom chatRoom, String userName) {
        return ChatRoomResponse.builder()
                .uuid(chatRoom.getUuid())
                .estimateRequestUuid(chatRoom.getEstimateRequest().getUuid())
                .estimateRequestTitle(chatRoom.getEstimateRequest().getTitle())
                .userId(chatRoom.getUser().getId())
                .userEmail(chatRoom.getUser().getEmail())
                .userName(userName != null ? userName : chatRoom.getUser().getEmail())
                .companyId(chatRoom.getCompany().getId())
                .companyUuid(chatRoom.getCompany().getUuid())
                .companyName(chatRoom.getCompany().getName())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCountUser(chatRoom.getUnreadCountUser())
                .unreadCountCompany(chatRoom.getUnreadCountCompany())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
