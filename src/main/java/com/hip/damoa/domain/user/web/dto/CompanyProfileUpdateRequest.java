package com.hip.damoa.domain.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMPANY 프로필 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileUpdateRequest {

    @NotBlank(message = "업체명은 필수입니다")
    @Size(max = 200, message = "업체명은 200자 이내로 입력해주세요")
    private String name;

    @Size(max = 2000, message = "업체 소개는 2000자 이내로 입력해주세요")
    private String description;

    @NotBlank(message = "대표 전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 02-1234-5678)")
    private String primaryPhone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자 이내로 입력해주세요")
    private String email;

    @Size(max = 500, message = "주소는 500자 이내로 입력해주세요")
    private String address;

    @Size(max = 20, message = "우편번호는 20자 이내로 입력해주세요")
    private String postalCode;
}
