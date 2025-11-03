package com.hip.damoa.domain.file.web.dto;

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
public class PresignedUrlResponse {

    private String uploadId;
    private String presignedUrl;
    private String fileKey;
    private String filename;
    private Long expiresIn; // seconds

    /**
     * 업로드 완료 후 호출할 URL
     */
    private String callbackUrl;
}
