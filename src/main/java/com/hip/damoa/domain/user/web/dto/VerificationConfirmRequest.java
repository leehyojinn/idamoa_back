package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationConfirmRequest {

    @NotBlank(message = "이메일 또는 전화번호는 필수입니다")
    private String target; // email or phone

    @NotBlank(message = "인증 코드는 필수입니다")
    private String code;
}
