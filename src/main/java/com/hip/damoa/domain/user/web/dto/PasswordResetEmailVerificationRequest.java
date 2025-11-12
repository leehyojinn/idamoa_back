package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 이메일 인증 코드 발송 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEmailVerificationRequest {

    @NotBlank(message = "재설정 토큰은 필수입니다")
    private String resetToken;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
