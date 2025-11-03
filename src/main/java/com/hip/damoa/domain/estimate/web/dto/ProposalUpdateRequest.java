package com.hip.damoa.domain.estimate.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposalUpdateRequest {

    @DecimalMin(value = "0.0", inclusive = true, message = "제안 금액은 0 이상이어야 합니다")
    private BigDecimal proposalAmount;

    @Min(value = 1, message = "예상 기간은 최소 1일 이상이어야 합니다")
    private Integer estimatedDurationDays;

    private String proposalContent;

    private String coverLetter;

    // Company info
    @Size(max = 100, message = "담당자 이름은 100자를 초과할 수 없습니다")
    private String contactPerson;

    @Size(max = 20, message = "연락처는 20자를 초과할 수 없습니다")
    private String contactPhone;

    @Email(message = "유효한 이메일 주소를 입력해주세요")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String contactEmail;

    // Portfolio
    private String portfolioDescription;
    private String previousProjects;

    private java.util.List<String> portfolioLinks;

    private java.time.LocalDate proposedStartDate;

    private java.time.LocalDate proposedEndDate;
}
