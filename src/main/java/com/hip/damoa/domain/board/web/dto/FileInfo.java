package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.file.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 파일 정보 DTO (Board Response용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    private UUID uuid;
    private String originalFilename;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String fileExtension;

    public static FileInfo from(File file) {
        return FileInfo.builder()
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileExtension(file.getFileExtension())
                .build();
    }
}
