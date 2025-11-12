package com.hip.damoa.domain.file.web.dto;

import com.hip.damoa.domain.file.model.File;
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
public class FileUploadResponse {

    private Long id;
    private UUID uuid;
    private String originalFilename;
    private String storedFilename;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String fileExtension;
    private String entityType;
    private Long entityId;
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
