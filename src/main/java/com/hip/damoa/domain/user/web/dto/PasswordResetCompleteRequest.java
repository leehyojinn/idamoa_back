package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 완료 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCompleteRequest {

    @NotBlank(message = "재설정 토큰은 필수입니다")
    private String resetToken;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String newPassword;
}
