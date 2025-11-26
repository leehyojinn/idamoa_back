package com.hip.damoa.domain.filter.web.dto;

import com.hip.damoa.domain.filter.model.FilterCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 필터 카테고리 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCategoryResponse {

    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private String filterType;
    private String description;
    private Integer displayOrder;
    private Boolean isRequired;
    private List<FilterOptionResponse> options;

    public static FilterCategoryResponse from(FilterCategory category, List<FilterOptionResponse> options) {
        return FilterCategoryResponse.builder()
                .id(category.getId())
                .uuid(category.getUuid())
                .code(category.getCode())
                .name(category.getName())
                .filterType(category.getFilterType())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isRequired(category.getIsRequired())
                .options(options)
                .build();
    }

    public static FilterCategoryResponse from(FilterCategory category) {
        return from(category, null);
    }
}
