package com.hip.damoa.domain.chat.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 엔티티
 *
 * 견적 요청자와 업체 간의 1:1 채팅방
 * - 하나의 견적 요청당 하나의 채팅방만 존재
 * - 양방향 실시간 메시지 전송
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms", indexes = {
        @Index(name = "idx_chat_rooms_estimate_request_id", columnList = "estimate_request_id"),
        @Index(name = "idx_chat_rooms_user_id", columnList = "user_id"),
        @Index(name = "idx_chat_rooms_company_id", columnList = "company_id"),
        @Index(name = "idx_chat_rooms_last_message_at", columnList = "last_message_at"),
        @Index(name = "idx_chat_rooms_uuid", columnList = "uuid")
})
public class ChatRoom extends BaseEntity {

    /**
     * 견적 요청 (어떤 견적 요청에 대한 채팅인지)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_request_id", nullable = false)
    private EstimateRequest estimateRequest;

    /**
     * 참여자 - 사용자 (견적 요청 작성자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 참여자 - 업체
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * 마지막 메시지 시각
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 마지막 메시지 내용 (미리보기용)
     */
    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    /**
     * 사용자 미읽음 개수
     */
    @Column(name = "unread_count_user", nullable = false)
    @Builder.Default
    private Integer unreadCountUser = 0;

    /**
     * 업체 미읽음 개수
     */
    @Column(name = "unread_count_company", nullable = false)
    @Builder.Default
    private Integer unreadCountCompany = 0;

    /**
     * 마지막 메시지 업데이트
     *
     * @param message 메시지 내용
     * @param senderType 발신자 타입
     */
    public void updateLastMessage(String message, SenderType senderType) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();

        // 발신자가 아닌 쪽의 미읽음 개수 증가
        if (senderType == SenderType.USER) {
            this.unreadCountCompany++;
        } else {
            this.unreadCountUser++;
        }
    }

    /**
     * 사용자 미읽음 개수 초기화
     */
    public void resetUserUnreadCount() {
        this.unreadCountUser = 0;
    }

    /**
     * 업체 미읽음 개수 초기화
     */
    public void resetCompanyUnreadCount() {
        this.unreadCountCompany = 0;
    }
}
