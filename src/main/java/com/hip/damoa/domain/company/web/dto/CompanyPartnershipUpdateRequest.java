package com.hip.damoa.domain.company.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 제휴업체 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPartnershipUpdateRequest {

    private Integer displayOrder;

    private LocalDate startDate;

    private LocalDate endDate;

    private String adminMemo;
}
