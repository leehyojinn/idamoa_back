package com.hip.damoa.domain.file.web.dto;

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
public class FileUploadCompleteRequest {

    @NotBlank(message = "Upload ID는 필수입니다")
    private String uploadId;

    @NotBlank(message = "File Key는 필수입니다")
    private String fileKey;

    private Integer width;

    private Integer height;

    private String description;
}
