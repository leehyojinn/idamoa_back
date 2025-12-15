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
import java.util.List;
import java.util.UUID;

/**
 * 일반 문의 상세 응답 DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일반 문의 상세 응답")
public class InquiryResponse {

    @Schema(description = "문의 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "문의 유형 (BUG, PAYMENT_ERROR, ACCOUNT_ISSUE, SUGGESTION, OTHER)", example = "SUGGESTION")
    private InquiryType inquiryType;

    @Schema(description = "문의 제목", example = "결제 관련 문의드립니다")
    private String title;

    @Schema(description = "문의 내용", example = "결제가 완료되었는데 크레딧이 충전되지 않았습니다.")
    private String content;

    @Schema(description = "문의 상태 (PENDING, IN_PROGRESS, RESOLVED, CLOSED)", example = "PENDING")
    private InquiryStatus status;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "답변 정보")
    private InquiryAnswerResponse answer;

    @Schema(description = "첨부파일 목록")
    private List<InquiryAttachmentResponse> attachments;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T14:00:00")
    private LocalDateTime updatedAt;

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
    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .uuid(inquiry.getUuid())
                .inquiryType(inquiry.getInquiryType())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .userEmail(getUserEmail(inquiry))
                .isDeleted(inquiry.getIsDeleted())
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
                .userEmail(getUserEmail(inquiry))
                .attachments(attachments)
                .isDeleted(inquiry.getIsDeleted())
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
                .userEmail(getUserEmail(inquiry))
                .answer(answer)
                .attachments(attachments)
                .isDeleted(inquiry.getIsDeleted())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
