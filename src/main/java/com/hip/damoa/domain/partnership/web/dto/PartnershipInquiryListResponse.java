package com.hip.damoa.domain.partnership.web.dto;

import com.hip.damoa.domain.partnership.model.PartnershipInquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제휴/광고 문의 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipInquiryListResponse {

    private UUID uuid;
    private String partnershipType;
    private String partnershipTypeDescription;
    private String name;
    private String email;
    private String status;
    private String statusDescription;
    private Boolean isDeleted;  // 삭제 여부 (관리자용)
    private LocalDateTime createdAt;

    public static PartnershipInquiryListResponse from(PartnershipInquiry inquiry) {
        return PartnershipInquiryListResponse.builder()
                .uuid(inquiry.getUuid())
                .partnershipType(inquiry.getPartnershipType().name())
                .partnershipTypeDescription(inquiry.getPartnershipType().getDescription())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .status(inquiry.getStatus().name())
                .statusDescription(inquiry.getStatus().getDescription())
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}