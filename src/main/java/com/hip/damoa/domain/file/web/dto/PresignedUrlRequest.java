package com.hip.damoa.domain.file.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "MIME 타입은 필수입니다")
    private String mimeType;

    @NotNull(message = "파일 크기는 필수입니다")
    private Long fileSize;

    private String entityType; // COMPANY_IMAGE, PORTFOLIO, etc.

    private Long entityId;
}
