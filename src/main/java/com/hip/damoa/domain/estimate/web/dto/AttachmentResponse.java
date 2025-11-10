package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateProposalAttachment;
import com.hip.damoa.domain.estimate.model.EstimateRequestAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 첨부파일 응답 DTO (EstimateRequest/Proposal 공통)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "첨부파일 응답")
public class AttachmentResponse {

    @Schema(description = "첨부파일 ID", example = "1")
    private Long id;

    @Schema(description = "파일 URL (File ID → URL 변환)", example = "https://bucket.s3.amazonaws.com/files/xxx.pdf")
    private String fileUrl;

    @Schema(description = "파일 타입", example = "DRAWING")
    private String fileType;

    @Schema(description = "파일 설명", example = "1층 평면도")
    private String fileDescription;

    @Schema(description = "표시 순서", example = "0")
    private Integer displayOrder;

    @Schema(description = "생성 일시", example = "2025-11-10T10:00:00")
    private LocalDateTime createdAt;

    // File 정보 (HTML에서 표시용)
    @Schema(description = "원본 파일명", example = "설계도.pdf")
    private String originalFilename;

    @Schema(description = "MIME 타입", example = "application/pdf")
    private String mimeType;

    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long fileSize;

    /**
     * EstimateRequestAttachment → AttachmentResponse 변환 (File 정보 포함)
     */
    public static AttachmentResponse from(EstimateRequestAttachment attachment, String fileUrl,
                                          String originalFilename, String mimeType, Long fileSize) {
        return AttachmentResponse.builder()
            .id(attachment.getId())
            .fileUrl(fileUrl)
            .fileType(attachment.getFileType())
            .fileDescription(attachment.getFileDescription())
            .displayOrder(attachment.getDisplayOrder())
            .createdAt(attachment.getCreatedAt())
            .originalFilename(originalFilename)
            .mimeType(mimeType)
            .fileSize(fileSize)
            .build();
    }

    /**
     * EstimateProposalAttachment → AttachmentResponse 변환 (File 정보 포함)
     */
    public static AttachmentResponse from(EstimateProposalAttachment attachment, String fileUrl,
                                          String originalFilename, String mimeType, Long fileSize) {
        return AttachmentResponse.builder()
            .id(attachment.getId())
            .fileUrl(fileUrl)
            .fileType(attachment.getFileType())
            .fileDescription(attachment.getFileDescription())
            .displayOrder(attachment.getDisplayOrder())
            .createdAt(attachment.getCreatedAt())
            .originalFilename(originalFilename)
            .mimeType(mimeType)
            .fileSize(fileSize)
            .build();
    }
}
