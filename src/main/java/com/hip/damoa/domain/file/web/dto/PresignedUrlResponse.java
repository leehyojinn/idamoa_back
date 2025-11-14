package com.hip.damoa.domain.file.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 생성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 생성 응답")
public class PresignedUrlResponse {

    @Schema(description = "업로드 ID (완료 처리 시 필요)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String uploadId;

    @Schema(description = "S3 업로드용 Presigned URL (이 URL로 PUT 요청)",
            example = "https://hip-damoa-uploads.s3.ap-northeast-2.amazonaws.com/uploads/2024/01/15/abc123.jpg?X-Amz-Algorithm=...")
    private String presignedUrl;

    @Schema(description = "S3 파일 키 (파일 식별자)",
            example = "uploads/2024/01/15/abc123.jpg")
    private String fileKey;

    @Schema(description = "원본 파일명",
            example = "profile.jpg")
    private String filename;

    @Schema(description = "URL 만료 시간 (초)",
            example = "3600")
    private Long expiresIn;

    @Schema(description = "업로드 완료 후 호출할 백엔드 API URL",
            example = "/api/files/complete")
    private String callbackUrl;
}
