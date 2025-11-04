package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsVerificationRequest {

    @NotBlank(message = "회원가입 토큰은 필수입니다")
    private String signupToken;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01(?:0|1|[6-9])[0-9]{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phoneNumber;
}
