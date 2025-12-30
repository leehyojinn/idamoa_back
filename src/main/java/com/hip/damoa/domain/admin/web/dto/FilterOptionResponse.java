package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
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
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String code;
    private String name;
    private String shortName;
    private String description;
    private Long parentId;
    private String parentCode;
    private String parentName;
    private Integer depth;
    private String path;
    private Map<String, Object> metadata;
    private Integer displayOrder;
    private String icon;
    private String color;
    private Boolean isActive;
    private Boolean isDefault;
    private Boolean isExpanded;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer childrenCount; // 자식 옵션 개수

    public static FilterOptionResponse from(FilterOption option) {
        return from(option, 0);
    }

    public static FilterOptionResponse from(FilterOption option, Integer childrenCount) {
        FilterOptionResponseBuilder builder = FilterOptionResponse.builder()
                .id(option.getId())
                .uuid(option.getUuid())
                .categoryId(option.getCategory().getId())
                .categoryCode(option.getCategory().getCode())
                .categoryName(option.getCategory().getName())
                .code(option.getCode())
                .name(option.getName())
                .shortName(option.getShortName())
                .description(option.getDescription())
                .depth(option.getDepth())
                .path(option.getPath())
                .metadata(option.getMetadata())
                .displayOrder(option.getDisplayOrder())
                .icon(option.getIcon())
                .color(option.getColor())
                .isActive(option.getIsActive())
                .isDefault(option.getIsDefault())
                .isExpanded(option.getIsExpanded())
                .usageCount(option.getUsageCount())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .childrenCount(childrenCount);

        if (option.getParent() != null) {
            builder.parentId(option.getParent().getId())
                    .parentCode(option.getParent().getCode())
                    .parentName(option.getParent().getName());
        }

        return builder.build();
    }
}