package com.hip.damoa.domain.file.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 파일 다운로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파일 다운로드 응답")
public class FileDownloadResponse {

    @Schema(description = "파일 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID fileUuid;

    @Schema(description = "파일명", example = "인테리어_가이드.pdf")
    private String fileName;

    @Schema(description = "다운로드 URL (Presigned URL)", example = "https://s3.ap-northeast-2.amazonaws.com/...")
    private String downloadUrl;

    @Schema(description = "결제된 금액 (무료: 0, 원)", example = "1000")
    private Integer price;
}
