package com.hip.damoa.domain.filter.web.dto;

import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 필터 옵션 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionResponse {

    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private String shortName;
    private String description;
    private Integer displayOrder;
    private String icon;
    private String color;
    private Integer usageCount;
    private Boolean isActive;

    // 계층 구조 정보
    private Integer depth;
    private Long parentId;
    private String path;
    @Builder.Default
    private List<FilterOptionResponse> children = new ArrayList<>();

    // 카테고리 정보
    private Long categoryId;
    private String categoryCode;
    private String categoryName;

    public static FilterOptionResponse from(FilterOption option) {
        return FilterOptionResponse.builder()
                .id(option.getId())
                .uuid(option.getUuid())
                .code(option.getCode())
                .name(option.getName())
                .shortName(option.getShortName())
                .description(option.getDescription())
                .displayOrder(option.getDisplayOrder())
                .icon(option.getIcon())
                .color(option.getColor())
                .usageCount(option.getUsageCount())
                .isActive(option.getIsActive())
                .depth(option.getDepth())
                .parentId(option.getParent() != null ? option.getParent().getId() : null)
                .path(option.getPath())
                .children(new ArrayList<>())
                .categoryId(option.getCategory() != null ? option.getCategory().getId() : null)
                .categoryCode(option.getCategory() != null ? option.getCategory().getCode() : null)
                .categoryName(option.getCategory() != null ? option.getCategory().getName() : null)
                .build();
    }

    /**
     * 계층 구조용 변환 (children 포함)
     */
    public static FilterOptionResponse fromWithChildren(FilterOption option, List<FilterOptionResponse> children) {
        FilterOptionResponse response = from(option);
        response.children = children != null ? children : new ArrayList<>();
        return response;
    }

    public void setChildren(List<FilterOptionResponse> children) {
        this.children = children;
    }
}
