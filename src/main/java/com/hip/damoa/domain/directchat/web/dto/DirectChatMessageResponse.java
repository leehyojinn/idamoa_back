package com.hip.damoa.domain.directchat.web.dto;

import com.hip.damoa.domain.directchat.model.DirectChatMessage;
import com.hip.damoa.domain.directchat.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 응답 DTO
 */
@Schema(description = "채팅 메시지 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatMessageResponse {

    @Schema(description = "메시지 UUID")
    private UUID uuid;

    @Schema(description = "채팅방 UUID")
    private UUID roomUuid;

    @Schema(description = "발신자 정보")
    private ParticipantInfo sender;

    @Schema(description = "메시지 타입", example = "TEXT")
    private MessageType messageType;

    @Schema(description = "메시지 내용")
    private String content;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentResponse> attachments;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "내가 보낸 메시지 여부")
    private Boolean isMine;

    /**
     * Entity에서 DTO 생성
     */
    public static DirectChatMessageResponse from(DirectChatMessage message) {
        return from(message, null);
    }

    /**
     * Entity에서 DTO 생성 (현재 사용자 ID 포함)
     */
    public static DirectChatMessageResponse from(DirectChatMessage message, Long currentUserId) {
        if (message == null) {
            return null;
        }

        List<AttachmentResponse> attachmentResponses = null;
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            attachmentResponses = message.getAttachments().stream()
                    .map(AttachmentResponse::from)
                    .collect(Collectors.toList());
        }

        boolean isMine = false;
        if (currentUserId != null && message.getSender() != null) {
            isMine = currentUserId.equals(message.getSender().getId());
        }

        return DirectChatMessageResponse.builder()
                .uuid(message.getUuid())
                .roomUuid(message.getRoom() != null ? message.getRoom().getUuid() : null)
                .sender(ParticipantInfo.from(message.getSender()))
                .messageType(message.getMessageType())
                .content(message.getContent())
                .attachments(attachmentResponses)
                .createdAt(message.getCreatedAt())
                .isMine(isMine)
                .build();
    }
}
