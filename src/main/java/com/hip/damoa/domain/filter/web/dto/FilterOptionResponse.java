package com.hip.damoa.domain.filter.web.dto;

import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필터 옵션 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionResponse {

    private Long id;
    private String code;
    private String name;
    private String value;
    private Integer displayOrder;
    private String iconUrl;
    private Long usageCount;

    public static FilterOptionResponse from(FilterOption option) {
        return FilterOptionResponse.builder()
                .id(option.getId())
                .code(option.getOptionCode())
                .name(option.getOptionName())
                .value(option.getOptionValue())
                .displayOrder(option.getDisplayOrder())
                .iconUrl(option.getIconUrl())
                .usageCount(option.getUsageCount())
                .build();
    }
}
