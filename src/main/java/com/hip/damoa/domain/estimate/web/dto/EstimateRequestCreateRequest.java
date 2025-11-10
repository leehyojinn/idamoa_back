package com.hip.damoa.domain.estimate.web.dto;

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
public class EstimateRequestCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @NotBlank(message = "견적 유형은 필수입니다")
    private String estimateType;

    // ===== 사업장/고객 정보 (V26) =====

    @Size(max = 200, message = "사업장명은 200자를 초과할 수 없습니다")
    private String clientName; // 사업장명/고객명 (예: "강남 치과", "홍대 카페", "역삼동 사무실")

    @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다")
    private String businessType; // 업종 (예: "치과", "카페", "사무실", "매장", "주거")

    // Location
    @Size(max = 255, message = "현장 주소는 255자를 초과할 수 없습니다")
    private String siteAddress;

    @Size(max = 50, message = "시/군/구는 50자를 초과할 수 없습니다")
    private String siteCity;

    @Size(max = 50, message = "시/도는 50자를 초과할 수 없습니다")
    private String siteState;

    // Project details
    @DecimalMin(value = "0.0", inclusive = false, message = "면적은 0보다 커야 합니다")
    private BigDecimal areaSqm;

    @DecimalMin(value = "0.0", inclusive = false, message = "평수는 0보다 커야 합니다")
    private BigDecimal areaPyeong; // 평수 (V26)

    @DecimalMin(value = "0.0", inclusive = true, message = "최소 예산은 0 이상이어야 합니다")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.0", inclusive = true, message = "최대 예산은 0 이상이어야 합니다")
    private BigDecimal budgetMax;

    // Timeline
    private LocalDate desiredStartDate;

    private LocalDate desiredCompletionDate;

    // Deadlines
    private LocalDateTime submissionDeadline;

    // Visibility
    @Builder.Default
    private Boolean isPublic = true;

    // ===== 연락처 정보 (V26) =====

    @Size(max = 100, message = "신청자 이름은 100자를 초과할 수 없습니다")
    private String contactName; // 신청자 이름

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
             message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String contactPhone; // 연락처

    // ===== 첨부파일 (V30: 조인 테이블 방식) =====

    private List<AttachmentRequest> attachments; // 첨부파일 목록
}
