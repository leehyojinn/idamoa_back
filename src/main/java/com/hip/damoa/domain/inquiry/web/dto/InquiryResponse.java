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
 * 문의 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponse {

    private Long id;
    private UUID uuid;
    private InquiryType inquiryType;
    private String name;
    private String email;
    private String phone;
    private String content;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
