package com.hip.damoa.domain.directchat.web.dto;

import com.hip.damoa.domain.directchat.model.DirectChatAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * 채팅 첨부파일 응답 DTO
 */
@Schema(description = "채팅 첨부파일 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    @Schema(description = "첨부파일 UUID")
    private UUID uuid;

    @Schema(description = "파일 UUID")
    private UUID fileUuid;

    @Schema(description = "원본 파일명", example = "document.pdf")
    private String originalFilename;

    @Schema(description = "파일 URL")
    private String fileUrl;

    @Schema(description = "파일 크기 (bytes)")
    private Long fileSize;

    @Schema(description = "MIME 타입", example = "application/pdf")
    private String mimeType;

    @Schema(description = "썸네일 URL (이미지인 경우)")
    private String thumbnailUrl;

    /**
     * Entity에서 DTO 생성
     */
    public static AttachmentResponse from(DirectChatAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return AttachmentResponse.builder()
                .uuid(attachment.getUuid())
                .fileUuid(attachment.getFile() != null ? attachment.getFile().getUuid() : null)
                .originalFilename(attachment.getOriginalFilename())
                .fileUrl(attachment.getFileUrl())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .thumbnailUrl(attachment.getThumbnailUrl())
                .build();
    }
}
