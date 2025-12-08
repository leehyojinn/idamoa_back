package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.file.model.File;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 파일 정보 DTO (Board Response용)
 */
@Schema(description = "파일 정보 (가격 정보 포함)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    @Schema(description = "파일 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "원본 파일명", example = "설계도면_v2.dwg")
    private String originalFilename;

    @Schema(description = "파일 URL (S3 Presigned URL)", example = "https://s3.ap-northeast-2.amazonaws.com/bucket/files/xxx.dwg")
    private String fileUrl;

    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    private Long fileSize;

    @Schema(description = "MIME 타입", example = "application/octet-stream")
    private String mimeType;

    @Schema(description = "파일 확장자", example = "dwg")
    private String fileExtension;

    // 가격 정보
    @Schema(description = "유료 파일 여부", example = "true")
    private Boolean isPaid;

    @Schema(description = "파일 가격 (원). isPaid=true일 때만 유효", example = "5000")
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
