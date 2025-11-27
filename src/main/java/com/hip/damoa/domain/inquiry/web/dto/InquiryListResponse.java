package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 일반 문의 목록 응답 DTO (간략 정보)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryListResponse {

    private UUID uuid;
    private InquiryType inquiryType;
    private String title;
    private InquiryStatus status;
    private String userEmail;
    private boolean hasAnswer;  // 답변 여부
    private Boolean isDeleted;  // 삭제 여부 (관리자용)
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .status(inquiry.getStatus())
                .userEmail(inquiry.getUser().getEmail())
                .hasAnswer(false)  // 기본값, Service에서 설정 필요
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (답변 여부 포함)
     */
    public static InquiryListResponse from(Inquiry inquiry, boolean hasAnswer) {
        return InquiryListResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .status(inquiry.getStatus())
                .userEmail(inquiry.getUser().getEmail())
                .hasAnswer(hasAnswer)
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
