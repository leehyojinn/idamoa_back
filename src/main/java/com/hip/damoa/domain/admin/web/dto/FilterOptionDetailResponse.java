package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 필터 옵션 상세 응답 DTO (자식 옵션 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionDetailResponse {

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
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<FilterOptionResponse> children = List.of(); // 자식 옵션 목록

    public static FilterOptionDetailResponse from(FilterOption option) {
        FilterOptionDetailResponseBuilder builder = FilterOptionDetailResponse.builder()
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
                .usageCount(option.getUsageCount())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt());

        if (option.getParent() != null) {
            builder.parentId(option.getParent().getId())
                    .parentCode(option.getParent().getCode())
                    .parentName(option.getParent().getName());
        }

        if (option.getChildren() != null && !option.getChildren().isEmpty()) {
            builder.children(option.getChildren().stream()
                    .map(FilterOptionResponse::from)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}