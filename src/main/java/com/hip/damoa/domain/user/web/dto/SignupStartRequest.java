package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "역할은 필수입니다")
    private String role;

    @NotNull(message = "이용약관 동의는 필수입니다")
    private Boolean termsAgreed;

    @NotNull(message = "개인정보 처리방침 동의는 필수입니다")
    private Boolean privacyAgreed;

    private Boolean marketingAgreed;

    // Role-specific fields
    private String name;
    private String phoneNumber;
    private String hospitalName;
    private String companyName;
    private String businessRegistrationNumber;
    private String designerName;
}
