package com.hip.damoa.domain.directchat.web.dto;

import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅방 응답 DTO
 */
@Schema(description = "채팅방 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatRoomResponse {

    @Schema(description = "채팅방 UUID")
    private UUID uuid;

    @Schema(description = "상대방 정보")
    private ParticipantInfo otherUser;

    @Schema(description = "마지막 메시지 내용")
    private String lastMessage;

    @Schema(description = "마지막 메시지 시간")
    private LocalDateTime lastMessageAt;

    @Schema(description = "미읽음 메시지 수")
    private Long unreadCount;

    @Schema(description = "활성 상태 (나가지 않은 상태)")
    private Boolean isActive;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    /**
     * Entity에서 DTO 생성
     */
    public static DirectChatRoomResponse from(DirectChatRoom room, Long currentUserId) {
        return from(room, currentUserId, 0L, false);
    }

    /**
     * Entity에서 DTO 생성 (미읽음 수, 온라인 상태 포함)
     */
    public static DirectChatRoomResponse from(DirectChatRoom room, Long currentUserId, Long unreadCount, boolean isOtherUserOnline) {
        if (room == null) {
            return null;
        }

        // 상대방 정보
        var otherUser = room.getOtherUser(currentUserId);
        ParticipantInfo otherUserInfo = ParticipantInfo.from(otherUser, isOtherUserOnline);

        return DirectChatRoomResponse.builder()
                .uuid(room.getUuid())
                .otherUser(otherUserInfo)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .isActive(room.isActiveForUser(currentUserId))
                .createdAt(room.getCreatedAt())
                .build();
    }
}
