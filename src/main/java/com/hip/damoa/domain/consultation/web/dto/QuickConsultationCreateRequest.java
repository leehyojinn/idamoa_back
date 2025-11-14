package com.hip.damoa.domain.consultation.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빠른상담 신청 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빠른상담 신청 요청")
public class QuickConsultationCreateRequest {

    // 신청자 정보
    @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    private String name;

    @Schema(description = "전화번호", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @Schema(description = "이메일 (선택)", example = "hong@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
    private String email;

    // 비밀번호 (비회원 조회용, 4자리)
    @Schema(description = "4자리 숫자 비밀번호 (비회원 조회용)", example = "1234")
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
    private String password;

    // 상담 내용
    @Schema(description = "제목 (선택)", example = "인테리어 견적 문의드립니다")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String subject;

    @Schema(description = "상담 내용", example = "50평 치과 인테리어 견적을 받고 싶습니다. 자세한 상담 부탁드립니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상담 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "상담 내용은 10자 이상 5000자 이하여야 합니다")
    private String message;

    @Schema(description = "선호 연락 방법 (선택)", example = "전화")
    @Size(max = 20, message = "선호 연락 방법은 20자 이하여야 합니다")
    private String preferredContactMethod;

    @Schema(description = "선호 연락 시간 (선택)", example = "오전 10시~12시")
    @Size(max = 100, message = "선호 연락 시간은 100자 이하여야 합니다")
    private String preferredContactTime;

    // 필수 동의 (4가지)
    @Schema(description = "개인정보 수집 및 이용 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "개인정보 수집 및 이용 동의는 필수입니다")
    @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다")
    private Boolean personalInfoConsent;

    @Schema(description = "개인정보 제3자 제공 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "개인정보 제3자 제공 동의는 필수입니다")
    @AssertTrue(message = "개인정보 제3자 제공에 동의해야 합니다")
    private Boolean thirdPartyConsent;

    @Schema(description = "이용약관 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "이용약관 동의는 필수입니다")
    @AssertTrue(message = "이용약관에 동의해야 합니다")
    private Boolean termsOfServiceConsent;

    // 선택 동의
    @Schema(description = "마케팅 수신 동의 (선택)", example = "false")
    private Boolean marketingConsent;

    // 약관 버전
    @Schema(description = "약관 버전 (선택)", example = "v1.0")
    @Size(max = 20, message = "약관 버전은 20자 이하여야 합니다")
    private String consentVersion;
}
