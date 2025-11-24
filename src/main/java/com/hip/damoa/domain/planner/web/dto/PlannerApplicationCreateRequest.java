package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.ConsultationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 플래너 신청서 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플래너 신청서 생성 요청")
public class PlannerApplicationCreateRequest {

    // 신청 정보
    @Schema(description = "신청서 제목", example = "강남역 근처 병원 인테리어 상담 요청")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "신청 내용 (상세 설명)", example = "병원 인테리어 리모델링을 계획하고 있습니다. 30평 규모이며, 현대적이고 깔끔한 분위기를 원합니다.")
    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Schema(description = "상담 방법", example = "VISIT", allowableValues = {"VISIT", "PHONE", "SNS"},
            implementation = String.class,
            enumAsRef = true)
    @NotNull(message = "상담 방법은 필수입니다")
    private ConsultationMethod consultationMethod;

    @Schema(description = "요청 내용 (1~5개 다중 선택 가능)",
            example = "[\"FULL_CONSULTING\", \"NEW_OPENING\"]",
            allowableValues = {"FULL_CONSULTING", "NEW_OPENING", "REMODELING", "OPERATION_CONSULTING", "LEGAL_INQUIRY"})
    @NotEmpty(message = "요청 내용은 최소 1개 이상 선택해야 합니다")
    @Size(min = 1, max = 5, message = "요청 내용은 1~5개 선택 가능합니다")
    private List<String> requestTypes;

    // 신청자 정보
    @Schema(description = "신청자 성함 (원장님 또는 담당자 이름)", example = "김원장")
    @NotBlank(message = "신청자 이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String applicantName;

    @Schema(description = "신청자 전화번호", example = "010-1234-5678")
    @NotBlank(message = "신청자 전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
            message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String applicantPhone;

    @Schema(description = "신청자 이메일", example = "doctor@example.com")
    @NotBlank(message = "신청자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자 이내여야 합니다")
    private String applicantEmail;

    // 사업장 정보 (선택)
    @Schema(description = "병원/사업장명 (선택)", example = "서울내과의원")
    @Size(max = 200, message = "사업장명은 200자 이내여야 합니다")
    private String businessName;

    @Schema(description = "병원/사업장 주소 (선택)", example = "서울시 강남구 테헤란로 123")
    private String businessAddress;

    @Schema(description = "병원/사업장 면적 (선택)", example = "30평")
    @Size(max = 50, message = "면적은 50자 이내여야 합니다")
    private String businessAreaSize;

    @Schema(description = "진료과목 또는 업종 (선택)", example = "내과, 피부과")
    @Size(max = 100, message = "업종은 100자 이내여야 합니다")
    private String businessType;

    // 첨부파일 (files 테이블 UUID 배열)
    @Schema(description = "첨부파일 UUID 목록 (전체 100MB 이하)", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    private List<String> attachmentFileUuids;

    // 희망 일정 (1~3개)
    @Schema(description = "희망 상담 일정 (1~3개, 우선순위별)", example = "[{\"priority\": 1, \"preferredDate\": \"2025-11-20\", \"preferredTime\": \"오전 10시\"}, {\"priority\": 2, \"preferredDate\": \"2025-11-21\", \"preferredTime\": \"오후 2시\"}]")
    @NotEmpty(message = "희망 일정은 최소 1개 이상 입력해야 합니다")
    @Size(min = 1, max = 3, message = "희망 일정은 1~3개까지 입력 가능합니다")
    @Valid
    private List<PreferredDateDto> preferredDates;
}
