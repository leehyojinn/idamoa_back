package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * 필터 카테고리 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCategoryUpdateRequest {

    @Size(max = 200, message = "이름은 200자를 초과할 수 없습니다")
    private String name;

    private String description;

    @Pattern(regexp = "^(SINGLE_SELECT|MULTI_SELECT|HIERARCHICAL)$", message = "유효하지 않은 필터 타입입니다")
    private String filterType;

    private Boolean supportsHierarchy;

    private Integer maxDepth;

    private Integer displayOrder;

    @Size(max = 100, message = "아이콘은 100자를 초과할 수 없습니다")
    private String icon;

    private Boolean isActive;

    private Boolean isRequired;

    private Map<String, Object> metadata;
}