package com.hip.damoa.domain.directchat.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 메시지 엔티티
 *
 * 채팅방 내 개별 메시지
 * - 발신자 (User)
 * - 메시지 타입 (TEXT, IMAGE, FILE, SYSTEM)
 * - 첨부파일 목록
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_chat_messages", indexes = {
        @Index(name = "idx_direct_chat_messages_room_id", columnList = "room_id"),
        @Index(name = "idx_direct_chat_messages_sender_id", columnList = "sender_id"),
        @Index(name = "idx_direct_chat_messages_uuid", columnList = "uuid")
})
public class DirectChatMessage extends BaseEntity {

    /**
     * 채팅방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DirectChatRoom room;

    /**
     * 발신자 (시스템 메시지인 경우 null)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private User sender;

    /**
     * 메시지 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /**
     * 메시지 내용
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 첨부파일 목록
     */
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DirectChatAttachment> attachments = new ArrayList<>();

    /**
     * 텍스트 메시지 생성
     */
    public static DirectChatMessage createTextMessage(DirectChatRoom room, User sender, String content) {
        return DirectChatMessage.builder()
                .room(room)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
    }

    /**
     * 이미지 메시지 생성
     */
    public static DirectChatMessage createImageMessage(DirectChatRoom room, User sender, String content) {
        return DirectChatMessage.builder()
                .room(room)
                .sender(sender)
                .messageType(MessageType.IMAGE)
                .content(content != null ? content : "")
                .build();
    }

    /**
     * 파일 메시지 생성
     */
    public static DirectChatMessage createFileMessage(DirectChatRoom room, User sender, String content) {
        return DirectChatMessage.builder()
                .room(room)
                .sender(sender)
                .messageType(MessageType.FILE)
                .content(content != null ? content : "")
                .build();
    }

    /**
     * 시스템 메시지 생성
     */
    public static DirectChatMessage createSystemMessage(DirectChatRoom room, String content) {
        return DirectChatMessage.builder()
                .room(room)
                .sender(null)
                .messageType(MessageType.SYSTEM)
                .content(content)
                .build();
    }

    /**
     * 첨부파일 추가
     */
    public void addAttachment(DirectChatAttachment attachment) {
        this.attachments.add(attachment);
    }
}
