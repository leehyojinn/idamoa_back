package com.hip.damoa.domain.consultation.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioConsultationAnswerRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    private String answer;
}
