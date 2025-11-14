package com.hip.damoa.domain.file.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 업로드 완료 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파일 업로드 완료 요청")
public class FileUploadCompleteRequest {

    @Schema(description = "업로드 ID (Presigned URL 응답에서 받은 값)",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Upload ID는 필수입니다")
    private String uploadId;

    @Schema(description = "파일 키 (Presigned URL 응답에서 받은 값)",
            example = "uploads/2024/01/15/abc123.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "File Key는 필수입니다")
    private String fileKey;

    @Schema(description = "이미지 가로 크기 (픽셀 - 이미지인 경우만)",
            example = "1920")
    private Integer width;

    @Schema(description = "이미지 세로 크기 (픽셀 - 이미지인 경우만)",
            example = "1080")
    private Integer height;

    @Schema(description = "파일 설명 (선택)",
            example = "회사 로고 이미지")
    private String description;
}
