package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ContactMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포트폴리오 상담신청 수정 요청 DTO
 *
 * 모든 필드는 선택사항입니다. 입력된 필드만 수정되고, null인 필드는 기존 값이 유지됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioConsultationUpdateRequest {

    private String name;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    private String title;

    private String content;

    private ContactMethod contactMethod;

    private String availableTime;
}
