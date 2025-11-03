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
public class SignupCompleteRequest {

    @NotBlank(message = "회원가입 토큰은 필수입니다")
    private String signupToken;

    @NotBlank(message = "이메일 인증 코드는 필수입니다")
    private String emailVerificationCode;

    @NotBlank(message = "SMS 인증 코드는 필수입니다")
    private String smsVerificationCode;
}
