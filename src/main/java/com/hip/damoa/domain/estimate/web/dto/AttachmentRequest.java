package com.hip.damoa.domain.estimate.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 첨부파일 요청 DTO (EstimateRequest/Proposal 공통)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "첨부파일 요청")
public class AttachmentRequest {

    @Schema(description = "파일 URL (S3 업로드 후 받은 URL, Service에서 File ID로 변환됨)", example = "https://bucket.s3.amazonaws.com/files/xxx.pdf")
    @NotBlank(message = "파일 URL은 필수입니다")
    private String fileUrl;

    @Schema(description = "파일 타입", example = "DRAWING", allowableValues = {"DRAWING", "PHOTO", "DOCUMENT", "ESTIMATE"})
    private String fileType;

    @Schema(description = "파일 설명 (선택사항)", example = "1층 평면도")
    private String fileDescription;

    @Schema(description = "표시 순서 (0부터 시작, 선택사항)", example = "0")
    private Integer displayOrder;
}
