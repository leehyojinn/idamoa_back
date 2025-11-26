package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.filter.model.FilterCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 필터 카테고리 상세 응답 DTO (옵션 목록 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCategoryDetailResponse {

    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private String description;
    private String entityType;
    private String filterType;
    private Boolean supportsHierarchy;
    private Integer maxDepth;
    private Integer displayOrder;
    private String icon;
    private Boolean isActive;
    private Boolean isRequired;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FilterOptionResponse> options; // 하위 옵션 목록

    public static FilterCategoryDetailResponse from(FilterCategory category, List<FilterOptionResponse> options) {
        return FilterCategoryDetailResponse.builder()
                .id(category.getId())
                .uuid(category.getUuid())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .entityType(category.getEntityType())
                .filterType(category.getFilterType())
                .supportsHierarchy(category.getSupportsHierarchy())
                .maxDepth(category.getMaxDepth())
                .displayOrder(category.getDisplayOrder())
                .icon(category.getIcon())
                .isActive(category.getIsActive())
                .isRequired(category.getIsRequired())
                .metadata(category.getMetadata())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .options(options)
                .build();
    }
}