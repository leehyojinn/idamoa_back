package com.hip.damoa.domain.directchat.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

/**
 * 채팅 읽음 상태 엔티티
 *
 * 각 사용자별 마지막으로 읽은 메시지 위치 추적
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_chat_read_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}),
       indexes = {
           @Index(name = "idx_direct_chat_read_states_room_user", columnList = "room_id, user_id")
       })
public class DirectChatReadState extends BaseTimeEntity {

    /**
     * 채팅방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DirectChatRoom room;

    /**
     * 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    /**
     * 마지막으로 읽은 메시지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private DirectChatMessage lastReadMessage;

    /**
     * 마지막 읽은 시간
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * 읽음 상태 생성
     */
    public static DirectChatReadState create(DirectChatRoom room, User user) {
        return DirectChatReadState.builder()
                .room(room)
                .user(user)
                .build();
    }

    /**
     * 읽음 처리
     */
    public void markAsRead(DirectChatMessage message) {
        this.lastReadMessage = message;
        this.lastReadAt = LocalDateTime.now();
    }

    /**
     * 특정 메시지를 읽었는지 확인
     */
    public boolean hasRead(DirectChatMessage message) {
        if (this.lastReadMessage == null) {
            return false;
        }
        return this.lastReadMessage.getId() >= message.getId();
    }
}
