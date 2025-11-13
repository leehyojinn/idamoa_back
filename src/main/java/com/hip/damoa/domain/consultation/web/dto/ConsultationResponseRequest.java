package com.hip.damoa.domain.consultation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상담 답변 작성 요청 DTO (관리자용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponseRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "답변 내용은 10자 이상 5000자 이하여야 합니다")
    private String responseMessage;
}
