package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewReplyRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    @Size(min = 10, max = 2000, message = "답변 내용은 10자 이상 2000자 이하여야 합니다")
    private String reply;
}
