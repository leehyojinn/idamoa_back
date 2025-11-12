package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 인증 코드 확인 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyRequest {

    @NotBlank(message = "재설정 토큰은 필수입니다")
    private String resetToken;

    @NotBlank(message = "인증 코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자입니다")
    private String code;
}
