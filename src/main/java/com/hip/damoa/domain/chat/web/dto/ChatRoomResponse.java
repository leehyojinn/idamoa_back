package com.hip.damoa.domain.chat.web.dto;

import com.hip.damoa.domain.chat.model.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅방 응답 DTO
 */
@Slf4j
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

    @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID userUuid;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

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
     * 삭제된 User의 경우 안전하게 처리
     */
    public static ChatRoomResponse from(ChatRoom chatRoom, String userName) {
        UUID userUuid = null;
        String userEmail = null;
        String resolvedUserName = userName;

        try {
            if (chatRoom.getUser() != null) {
                userUuid = chatRoom.getUser().getUuid();
                userEmail = chatRoom.getUser().getEmail();
                if (resolvedUserName == null) {
                    resolvedUserName = userEmail;
                }
            }
        } catch (Exception e) {
            log.warn("User not found for chatRoom: {}", chatRoom.getId());
            userEmail = "알 수 없음";
            resolvedUserName = "알 수 없음";
        }

        return ChatRoomResponse.builder()
                .uuid(chatRoom.getUuid())
                .estimateRequestUuid(chatRoom.getEstimateRequest().getUuid())
                .estimateRequestTitle(chatRoom.getEstimateRequest().getTitle())
                .userUuid(userUuid)
                .userEmail(userEmail)
                .userName(resolvedUserName)
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
