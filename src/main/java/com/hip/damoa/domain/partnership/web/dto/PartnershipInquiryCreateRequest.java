package com.hip.damoa.domain.partnership.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제휴/광고 문의 생성 요청 DTO
 */
@Schema(description = "제휴/광고 문의 생성 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipInquiryCreateRequest {

    @Schema(description = "문의 유형\n\n" +
            "- `PARTNERSHIP`: 제휴 문의\n" +
            "- `ADVERTISEMENT`: 광고 문의\n" +
            "- `OTHER`: 기타 문의",
            example = "PARTNERSHIP",
            allowableValues = {"PARTNERSHIP", "ADVERTISEMENT", "OTHER"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "문의 유형은 필수입니다")
    @Pattern(regexp = "^(PARTNERSHIP|ADVERTISEMENT|OTHER)$",
            message = "문의 유형은 PARTNERSHIP, ADVERTISEMENT, OTHER 중 하나여야 합니다")
    private String partnershipType;

    @Schema(description = "문의자 이름",
            example = "홍길동",
            minLength = 2,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "작성자명은 필수입니다")
    private String name;

    @Schema(description = "문의자 이메일 주소",
            example = "contact@example.com",
            pattern = "^[A-Za-z0-9+_.-]+@(.+)$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "문의자 연락처 (하이픈 포함)",
            example = "010-1234-5678",
            pattern = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
            message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @Schema(description = "문의 내용 (제휴 제안, 광고 문의 등 상세 내용)",
            example = "안녕하세요. 저희 회사와 제휴를 제안드리고자 연락드립니다...",
            minLength = 10,
            maxLength = 5000,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "문의 내용은 필수입니다")
    private String content;
}