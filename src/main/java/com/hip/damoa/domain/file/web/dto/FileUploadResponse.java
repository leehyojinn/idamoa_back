package com.hip.damoa.domain.file.web.dto;

import com.hip.damoa.domain.file.model.File;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 파일 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파일 업로드 응답")
public class FileUploadResponse {

    @Schema(description = "파일 ID", example = "123")
    private Long id;

    @Schema(description = "파일 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "원본 파일명", example = "profile.jpg")
    private String originalFilename;

    @Schema(description = "저장된 파일명", example = "abc123.jpg")
    private String storedFilename;

    @Schema(description = "파일 접근 URL (CDN 또는 S3 URL)",
            example = "https://hip-damoa-uploads.s3.ap-northeast-2.amazonaws.com/uploads/2024/01/15/abc123.jpg")
    private String fileUrl;

    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    private Long fileSize;

    @Schema(description = "MIME 타입", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "파일 확장자", example = "jpg")
    private String fileExtension;

    @Schema(description = "연결된 엔티티 타입", example = "COMPANY_IMAGE")
    private String entityType;

    @Schema(description = "연결된 엔티티 ID", example = "123")
    private Long entityId;

    @Schema(description = "파일 업로드 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    public static FileUploadResponse from(File file) {
        return FileUploadResponse.builder()
                .id(file.getId())
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileExtension(file.getFileExtension())
                .entityType(file.getEntityType())
                .entityId(file.getEntityId())
                .createdAt(file.getCreatedAt())
                .build();
    }
}
