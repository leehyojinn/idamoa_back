package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.filter.model.FilterCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * 필터 카테고리 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCategoryCreateRequest {

    @NotBlank(message = "카테고리 코드는 필수입니다")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "코드는 소문자, 숫자, 언더스코어만 가능하며 소문자로 시작해야 합니다")
    @Size(max = 100, message = "코드는 100자를 초과할 수 없습니다")
    private String code;

    @NotBlank(message = "카테고리 이름은 필수입니다")
    @Size(max = 200, message = "이름은 200자를 초과할 수 없습니다")
    private String name;

    private String description;

    @NotBlank(message = "엔티티 타입은 필수입니다")
    @Pattern(regexp = "^(COMPANY|BOARD|SERVICE|HOSPITAL)$", message = "유효하지 않은 엔티티 타입입니다")
    private String entityType;

    @NotBlank(message = "필터 타입은 필수입니다")
    @Pattern(regexp = "^(SINGLE_SELECT|MULTI_SELECT|HIERARCHICAL)$", message = "유효하지 않은 필터 타입입니다")
    private String filterType;

    @Builder.Default
    private Boolean supportsHierarchy = false;

    @Builder.Default
    private Integer maxDepth = 1;

    @Builder.Default
    private Integer displayOrder = 0;

    @Size(max = 100, message = "아이콘은 100자를 초과할 수 없습니다")
    private String icon;

    @NotNull(message = "활성 상태는 필수입니다")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isRequired = false;

    private Map<String, Object> metadata;

    public FilterCategory toEntity() {
        return FilterCategory.builder()
                .code(code)
                .name(name)
                .description(description)
                .entityType(entityType)
                .filterType(filterType)
                .supportsHierarchy(supportsHierarchy)
                .maxDepth(maxDepth)
                .displayOrder(displayOrder)
                .icon(icon)
                .isActive(isActive)
                .isRequired(isRequired)
                .build();
    }
}