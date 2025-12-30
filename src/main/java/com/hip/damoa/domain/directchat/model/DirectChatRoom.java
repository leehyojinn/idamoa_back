package com.hip.damoa.domain.directchat.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

/**
 * 1:1 채팅방 엔티티
 *
 * 범용 1:1 채팅을 위한 채팅방
 * - user1_id < user2_id 규칙으로 중복 방지
 * - 양방향 실시간 메시지 전송
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_chat_rooms", indexes = {
        @Index(name = "idx_direct_chat_rooms_user1_id", columnList = "user1_id"),
        @Index(name = "idx_direct_chat_rooms_user2_id", columnList = "user2_id"),
        @Index(name = "idx_direct_chat_rooms_last_message_at", columnList = "last_message_at"),
        @Index(name = "idx_direct_chat_rooms_uuid", columnList = "uuid")
})
public class DirectChatRoom extends BaseEntity {

    /**
     * 참여자1 (항상 user1_id < user2_id 규칙)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user1;

    /**
     * 참여자2
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user2;

    /**
     * 마지막 메시지 내용 (미리보기용)
     */
    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    /**
     * 마지막 메시지 시각
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 마지막 메시지 발신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_sender_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private User lastSender;

    /**
     * user1 활성 상태 (나가기 여부)
     */
    @Column(name = "user1_active", nullable = false)
    @Builder.Default
    private Boolean user1Active = true;

    /**
     * user2 활성 상태 (나가기 여부)
     */
    @Column(name = "user2_active", nullable = false)
    @Builder.Default
    private Boolean user2Active = true;

    /**
     * 채팅방 생성 (user1_id < user2_id 규칙 적용)
     */
    public static DirectChatRoom create(User userA, User userB) {
        User user1 = userA.getId() < userB.getId() ? userA : userB;
        User user2 = userA.getId() < userB.getId() ? userB : userA;

        return DirectChatRoom.builder()
                .user1(user1)
                .user2(user2)
                .user1Active(true)
                .user2Active(true)
                .build();
    }

    /**
     * 상대방 사용자 조회
     */
    public User getOtherUser(Long myUserId) {
        if (user1 != null && user1.getId().equals(myUserId)) {
            return user2;
        }
        return user1;
    }

    /**
     * 내 활성 상태 조회
     */
    public boolean isActiveForUser(Long userId) {
        if (user1 != null && user1.getId().equals(userId)) {
            return Boolean.TRUE.equals(user1Active);
        }
        return Boolean.TRUE.equals(user2Active);
    }

    /**
     * 마지막 메시지 업데이트
     */
    public void updateLastMessage(String message, User sender) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();
        this.lastSender = sender;
    }

    /**
     * 채팅방 나가기
     */
    public void leaveRoom(Long userId) {
        if (user1 != null && user1.getId().equals(userId)) {
            this.user1Active = false;
        } else if (user2 != null && user2.getId().equals(userId)) {
            this.user2Active = false;
        }
    }

    /**
     * 채팅방 재활성화 (메시지 수신 시)
     */
    public void reactivateRoom(Long userId) {
        if (user1 != null && user1.getId().equals(userId)) {
            this.user1Active = true;
        } else if (user2 != null && user2.getId().equals(userId)) {
            this.user2Active = true;
        }
    }

    /**
     * 채팅방 참여 여부 확인
     */
    public boolean isParticipant(Long userId) {
        return (user1 != null && user1.getId().equals(userId)) ||
               (user2 != null && user2.getId().equals(userId));
    }
}
