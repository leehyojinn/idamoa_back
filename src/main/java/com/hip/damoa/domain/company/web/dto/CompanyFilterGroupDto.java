package com.hip.damoa.domain.company.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카테고리별로 그룹화된 필터 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyFilterGroupDto {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryDescription;
    private List<FilterOptionDto> options;
}