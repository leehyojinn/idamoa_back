package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.InquiryAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 일반 문의 답변 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일반 문의 답변 응답")
public class InquiryAnswerResponse {

    @Schema(description = "답변 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID uuid;

    @Schema(description = "답변 작성 관리자 이메일", example = "admin@example.com")
    private String adminEmail;

    @Schema(description = "답변 내용", example = "문의 주신 내용에 대한 답변입니다...")
    private String content;

    @Schema(description = "답변 작성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "답변 수정일시", example = "2024-01-01T11:00:00")
    private LocalDateTime updatedAt;

    /**
     * InquiryAnswer 엔티티를 DTO로 변환
     */
    public static InquiryAnswerResponse from(InquiryAnswer answer) {
        return InquiryAnswerResponse.builder()
                .uuid(answer.getUuid())
                .adminEmail(answer.getAdmin().getEmail())
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }
}