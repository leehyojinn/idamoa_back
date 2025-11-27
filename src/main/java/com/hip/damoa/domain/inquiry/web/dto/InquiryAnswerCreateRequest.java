package com.hip.damoa.domain.inquiry.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일반 문의 답변 작성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일반 문의 답변 작성 요청")
public class InquiryAnswerCreateRequest {

    @Schema(description = "답변 내용", example = "문의 주신 내용에 대한 답변입니다...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "답변 내용은 필수입니다")
    private String content;
}