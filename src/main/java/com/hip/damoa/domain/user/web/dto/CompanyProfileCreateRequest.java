package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMPANY 프로필 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileCreateRequest {

    @NotBlank(message = "업체명은 필수입니다")
    private String name;

    private String description;

    @NotBlank(message = "대표 전화번호는 필수입니다")
    @Pattern(regexp = "^0\\d{1,2}-\\d{3,4}-\\d{4}$",
            message = "올바른 전화번호 형식이 아닙니다 (예: 02-1234-5678)")
    private String primaryPhone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    private String address;
    private String postalCode;
}
