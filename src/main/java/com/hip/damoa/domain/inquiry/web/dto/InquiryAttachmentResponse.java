package com.hip.damoa.domain.inquiry.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 문의 첨부파일 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryAttachmentResponse {

    private UUID fileUuid;
    private String fileName;
    private String fileType;
    private String fileDescription;
    private Long fileSize;
    private String downloadUrl;
    private Integer displayOrder;

    /**
     * File Entity + InquiryAttachment → DTO 변환
     */
    public static InquiryAttachmentResponse from(com.hip.damoa.domain.file.model.File file,
                                                  com.hip.damoa.domain.inquiry.model.InquiryAttachment attachment) {
        return InquiryAttachmentResponse.builder()
                .fileUuid(file.getUuid())
                .fileName(file.getOriginalFilename())
                .fileType(attachment.getFileType() != null ? attachment.getFileType() : file.getMimeType())
                .fileDescription(attachment.getFileDescription())
                .fileSize(file.getFileSize())
                .downloadUrl(file.getFileUrl())  // S3 URL
                .displayOrder(attachment.getDisplayOrder())
                .build();
    }
}