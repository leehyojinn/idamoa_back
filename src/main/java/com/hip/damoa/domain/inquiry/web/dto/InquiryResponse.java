package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 일반 문의 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponse {

    private UUID uuid;
    private InquiryType inquiryType;
    private String title;
    private String content;
    private InquiryStatus status;
    private String userEmail;  // 작성자 이메일
    private InquiryAnswerResponse answer;  // 답변 정보 (있는 경우)
    private List<InquiryAttachmentResponse> attachments;  // 첨부파일 목록
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .userEmail(inquiry.getUser().getEmail())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (첨부파일 포함)
     */
    public static InquiryResponse from(Inquiry inquiry, List<InquiryAttachmentResponse> attachments) {
        return InquiryResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .userEmail(inquiry.getUser().getEmail())
                .attachments(attachments)
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (답변 및 첨부파일 포함)
     */
    public static InquiryResponse from(Inquiry inquiry, InquiryAnswerResponse answer, List<InquiryAttachmentResponse> attachments) {
        return InquiryResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .userEmail(inquiry.getUser().getEmail())
                .answer(answer)
                .attachments(attachments)
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
