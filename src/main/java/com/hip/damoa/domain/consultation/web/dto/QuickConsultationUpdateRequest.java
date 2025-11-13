package com.hip.damoa.domain.consultation.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빠른상담 수정 요청 DTO
 *
 * SUBMITTED 상태일 때만 수정 가능
 * 동의 항목은 수정 불가 (최초 신청 시에만 설정 가능)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickConsultationUpdateRequest {

    // 신청자 정보
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
    private String email;

    // 상담 내용
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String subject;

    @NotBlank(message = "상담 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "상담 내용은 10자 이상 5000자 이하여야 합니다")
    private String message;

    @Size(max = 20, message = "선호 연락 방법은 20자 이하여야 합니다")
    private String preferredContactMethod;

    @Size(max = 100, message = "선호 연락 시간은 100자 이하여야 합니다")
    private String preferredContactTime;

    // 비밀번호 (비회원 수정 시 본인 확인용)
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
    private String password;
}
