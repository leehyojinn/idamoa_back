package com.hip.damoa.infra.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Upload Result DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResult {
    private String originalFilename;
    private String storedFilename;
    private String s3Key;
    private String s3Url;
    private Long fileSize;
    private String contentType;
    private String fileExtension;
}
