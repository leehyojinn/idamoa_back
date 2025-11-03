package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * 견적 관련 메시지
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_messages", indexes = {
    @Index(name = "idx_estimate_messages_request_id", columnList = "request_id"),
    @Index(name = "idx_estimate_messages_proposal_id", columnList = "proposal_id"),
    @Index(name = "idx_estimate_messages_sender_id", columnList = "sender_id")
})
public class EstimateMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private EstimateRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private EstimateProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    @Type(StringArrayType.class)
    @Column(name = "attachments", columnDefinition = "text[]")
    private String[] attachments;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "message_type", length = 20)
    @Builder.Default
    private String messageType = "GENERAL"; // GENERAL, QUESTION, ANSWER, NEGOTIATION

    @Column(name = "parent_message_id")
    private Long parentMessageId;

    // ===== Business Methods =====

    /**
     * 메시지 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
