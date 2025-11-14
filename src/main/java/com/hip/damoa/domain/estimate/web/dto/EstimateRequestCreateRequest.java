package com.hip.damoa.domain.estimate.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "견적 요청 생성 요청")
public class EstimateRequestCreateRequest {

    @Schema(description = "견적 요청 제목", example = "강남 치과 인테리어 견적 요청")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "상세 설명", example = "50평 규모의 치과 인테리어 공사를 진행하려고 합니다. 현대적이고 깔끔한 분위기를 원합니다.")
    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @Schema(description = "견적 유형 (인테리어/건축/리모델링 등)", example = "인테리어")
    @NotBlank(message = "견적 유형은 필수입니다")
    private String estimateType;

    // ===== 사업장/고객 정보 (V26) =====

    @Schema(description = "사업장명 또는 고객명 (선택)", example = "강남 치과의원")
    @Size(max = 200, message = "사업장명은 200자를 초과할 수 없습니다")
    private String clientName;

    @Schema(description = "업종 (선택)", example = "치과")
    @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다")
    private String businessType;

    // Location
    @Schema(description = "현장 주소 (선택)", example = "서울시 강남구 테헤란로 123")
    @Size(max = 255, message = "현장 주소는 255자를 초과할 수 없습니다")
    private String siteAddress;

    @Schema(description = "시/군/구 (선택)", example = "강남구")
    @Size(max = 50, message = "시/군/구는 50자를 초과할 수 없습니다")
    private String siteCity;

    @Schema(description = "시/도 (선택)", example = "서울시")
    @Size(max = 50, message = "시/도는 50자를 초과할 수 없습니다")
    private String siteState;

    // Project details
    @Schema(description = "면적(제곱미터) (선택)", example = "165.29")
    @DecimalMin(value = "0.0", inclusive = false, message = "면적은 0보다 커야 합니다")
    private BigDecimal areaSqm;

    @Schema(description = "평수 (선택)", example = "50")
    @DecimalMin(value = "0.0", inclusive = false, message = "평수는 0보다 커야 합니다")
    private BigDecimal areaPyeong;

    @Schema(description = "최소 예산 (원) (선택)", example = "50000000")
    @DecimalMin(value = "0.0", inclusive = true, message = "최소 예산은 0 이상이어야 합니다")
    private BigDecimal budgetMin;

    @Schema(description = "최대 예산 (원) (선택)", example = "100000000")
    @DecimalMin(value = "0.0", inclusive = true, message = "최대 예산은 0 이상이어야 합니다")
    private BigDecimal budgetMax;

    // Timeline
    @Schema(description = "희망 시작일 (선택)", example = "2025-12-01")
    private LocalDate desiredStartDate;

    @Schema(description = "희망 완료일 (선택)", example = "2026-02-28")
    private LocalDate desiredCompletionDate;

    // Deadlines
    @Schema(description = "제안 제출 마감일 (선택)", example = "2025-11-30T23:59:59")
    private LocalDateTime submissionDeadline;

    // Visibility
    @Schema(description = "공개 여부 (기본값: true)", example = "true")
    @Builder.Default
    private Boolean isPublic = true;

    // ===== 연락처 정보 (V26) =====

    @Schema(description = "신청자 이름 (선택)", example = "김원장")
    @Size(max = 100, message = "신청자 이름은 100자를 초과할 수 없습니다")
    private String contactName;

    @Schema(description = "연락처 (선택)", example = "010-1234-5678")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
             message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String contactPhone;

    // ===== 첨부파일 (V30: 조인 테이블 방식) =====

    @Schema(description = "첨부파일 목록 (도면, 사진 등)")
    private List<AttachmentRequest> attachments;
}
