package com.hip.damoa.domain.estimate.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateRequestUpdateRequest {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    private String description;

    private String estimateType;

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
    private Boolean isPublic;
}
