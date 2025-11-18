package com.hip.damoa.domain.chat.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 *
 * 채팅방 내 개별 메시지
 * - 발신자 타입 (USER or COMPANY)
 * - 메시지 내용
 * - 읽음 상태
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_chat_room_id", columnList = "chat_room_id"),
        @Index(name = "idx_chat_messages_sender_type", columnList = "sender_type"),
        @Index(name = "idx_chat_messages_is_read", columnList = "is_read"),
        @Index(name = "idx_chat_messages_created_at", columnList = "created_at"),
        @Index(name = "idx_chat_messages_uuid", columnList = "uuid")
})
public class ChatMessage extends BaseEntity {

    /**
     * 채팅방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 발신자 타입 (USER or COMPANY)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    /**
     * 발신자 ID
     * - senderType == USER: User ID
     * - senderType == COMPANY: Company ID
     */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * 메시지 내용
     */
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 시각
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
