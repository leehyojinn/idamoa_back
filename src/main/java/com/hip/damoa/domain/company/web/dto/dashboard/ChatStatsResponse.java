package com.hip.damoa.domain.company.web.dto.dashboard;

import com.hip.damoa.domain.directchat.model.DirectChatRoom;
import com.hip.damoa.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 채팅 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatsResponse {

    private long activeCount;   // 활성 채팅방 수
    private long unreadCount;   // 읽지 않은 메시지 총 수

    private List<RecentChat> recentChats;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentChat {
        private UUID chatRoomUuid;
        private String partnerName;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private long unreadCount;

        public static RecentChat from(DirectChatRoom room, Long currentUserId, long unreadCount) {
            User partner = room.getUser1().getId().equals(currentUserId)
                    ? room.getUser2()
                    : room.getUser1();

            String partnerName = partner != null ? partner.getEmail() : "알수없음";

            return RecentChat.builder()
                    .chatRoomUuid(room.getUuid())
                    .partnerName(partnerName)
                    .lastMessage(room.getLastMessage())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    /**
     * 요약용 (전체 대시보드 요약에 포함)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long activeCount;
        private long unreadCount;
    }
}
