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

    // 가격 정보
    private Boolean isPaid;
    private Integer price;

    public static FileInfo from(File file) {
        return FileInfo.builder()
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileExtension(file.getFileExtension())
                .isPaid(false)
                .price(0)
                .build();
    }

    /**
     * 파일과 가격 정보를 포함하여 FileInfo 생성
     */
    public static FileInfo from(File file, Boolean isPaid, Integer price) {
        return FileInfo.builder()
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileExtension(file.getFileExtension())
                .isPaid(isPaid != null ? isPaid : false)
                .price(price != null ? price : 0)
                .build();
    }
}
