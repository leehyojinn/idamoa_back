package com.hip.damoa.domain.consultation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빠른상담 비밀번호 검증 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPasswordRequest {

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
    private String password;
}
