package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 제휴업체 등록 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPartnershipCreateRequest {

    @NotNull(message = "업체 UUID는 필수입니다")
    private UUID companyUuid;

    private Integer displayOrder;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "만료일은 필수입니다")
    private LocalDate endDate;

    private String adminMemo;
}
