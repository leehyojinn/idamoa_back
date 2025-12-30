package com.hip.damoa.domain.directchat.web.dto;

import com.hip.damoa.domain.directchat.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Schema(description = "채팅 메시지 전송 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatMessageRequest {

    @Schema(description = "메시지 내용 (최대 10,000자)", example = "안녕하세요!", maxLength = 10000)
    @Size(max = 10000, message = "메시지는 10,000자를 초과할 수 없습니다")
    private String content;

    @Schema(description = "메시지 타입", example = "TEXT")
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Schema(description = "첨부파일 UUID 목록 (최대 10개)")
    @Size(max = 10, message = "첨부파일은 최대 10개까지 가능합니다")
    private List<UUID> fileUuids;
}
