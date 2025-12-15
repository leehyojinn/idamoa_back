package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 일반 문의 목록 응답 DTO (간략 정보)
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일반 문의 목록 응답")
public class InquiryListResponse {

    @Schema(description = "문의 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "문의 유형 (BUG, PAYMENT_ERROR, ACCOUNT_ISSUE, SUGGESTION, OTHER)", example = "SUGGESTION")
    private InquiryType inquiryType;

    @Schema(description = "문의 제목", example = "결제 관련 문의드립니다")
    private String title;

    @Schema(description = "문의 상태 (PENDING, IN_PROGRESS, RESOLVED, CLOSED)", example = "PENDING")
    private InquiryStatus status;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "답변 여부", example = "false")
    private boolean hasAnswer;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 사용자 이메일 안전하게 조회 (삭제된 사용자 처리)
     */
    private static String getUserEmail(Inquiry inquiry) {
        try {
            return inquiry.getUser() != null ? inquiry.getUser().getEmail() : null;
        } catch (Exception e) {
            log.warn("User not found for inquiry: {}", inquiry.getId());
            return "[삭제된 사용자]";
        }
    }

    /**
     * Entity → DTO 변환
     */
    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .status(inquiry.getStatus())
                .userEmail(getUserEmail(inquiry))
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
                .userEmail(getUserEmail(inquiry))
                .hasAnswer(hasAnswer)
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
