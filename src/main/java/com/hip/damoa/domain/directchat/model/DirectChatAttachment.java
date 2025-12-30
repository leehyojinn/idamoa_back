package com.hip.damoa.domain.directchat.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.file.model.File;
import jakarta.persistence.*;
import lombok.*;

/**
 * 채팅 첨부파일 엔티티
 *
 * 채팅 메시지에 첨부된 파일 정보
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_chat_attachments", indexes = {
        @Index(name = "idx_direct_chat_attachments_message_id", columnList = "message_id"),
        @Index(name = "idx_direct_chat_attachments_file_id", columnList = "file_id")
})
public class DirectChatAttachment extends BaseEntity {

    /**
     * 메시지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private DirectChatMessage message;

    /**
     * 파일 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    /**
     * 원본 파일명
     */
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    /**
     * 파일 URL
     */
    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    /**
     * 파일 크기 (bytes)
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * MIME 타입
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 썸네일 URL (이미지인 경우)
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * 첨부파일 생성
     */
    public static DirectChatAttachment create(DirectChatMessage message, File file) {
        return DirectChatAttachment.builder()
                .message(message)
                .file(file)
                .originalFilename(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .thumbnailUrl(null) // 필요 시 별도 생성
                .build();
    }
}
