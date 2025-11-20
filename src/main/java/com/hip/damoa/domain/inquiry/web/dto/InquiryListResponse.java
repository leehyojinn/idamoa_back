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
 * 문의 목록 응답 DTO (간략 정보)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryListResponse {

    private Long id;
    private UUID uuid;
    private InquiryType inquiryType;
    private String name;
    private String email;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .id(inquiry.getId())
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
