package com.hip.damoa.domain.partnership.web.dto;

import com.hip.damoa.domain.partnership.model.PartnershipInquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제휴/광고 문의 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제휴/광고 문의 상세 응답")
public class PartnershipInquiryResponse {

    @Schema(description = "문의 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "제휴 유형 (ADVERTISING, PARTNERSHIP, OTHER)", example = "ADVERTISING")
    private String partnershipType;

    @Schema(description = "제휴 유형 설명", example = "광고 문의")
    private String partnershipTypeDescription;

    @Schema(description = "신청자 이름", example = "홍길동")
    private String name;

    @Schema(description = "신청자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phone;

    @Schema(description = "문의 내용", example = "광고 협업 관련해서 문의드립니다.")
    private String content;

    @Schema(description = "처리 상태 (PENDING, IN_PROGRESS, COMPLETED, REJECTED)", example = "PENDING")
    private String status;

    @Schema(description = "처리 상태 설명", example = "대기 중")
    private String statusDescription;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T14:00:00")
    private LocalDateTime updatedAt;

    public static PartnershipInquiryResponse from(PartnershipInquiry inquiry) {
        return PartnershipInquiryResponse.builder()
                .uuid(inquiry.getUuid())
                .partnershipType(inquiry.getPartnershipType().name())
                .partnershipTypeDescription(inquiry.getPartnershipType().getDescription())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .content(inquiry.getContent())
                .status(inquiry.getStatus().name())
                .statusDescription(inquiry.getStatus().getDescription())
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}