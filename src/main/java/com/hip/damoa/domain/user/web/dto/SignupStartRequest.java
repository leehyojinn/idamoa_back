package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 시작 요청 DTO
 * 이메일/비밀번호 + 약관동의만 필요
 * 프로필 정보는 회원가입 완료 후 별도 입력
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupStartRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;

    @NotNull(message = "이용약관 동의는 필수입니다")
    private Boolean termsAgreed;

    @NotNull(message = "개인정보 처리방침 동의는 필수입니다")
    private Boolean privacyAgreed;

    private Boolean marketingAgreed;
}
