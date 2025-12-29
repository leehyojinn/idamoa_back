package com.hip.damoa.domain.admin.web.dto;

import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * 필터 옵션 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다")
    private Long categoryId;

    @NotBlank(message = "옵션 코드는 필수입니다")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "코드는 소문자, 숫자, 언더스코어, 하이픈만 가능하며 소문자로 시작해야 합니다")
    @Size(max = 100, message = "코드는 100자를 초과할 수 없습니다")
    private String code;

    @NotBlank(message = "옵션 이름은 필수입니다")
    @Size(max = 200, message = "이름은 200자를 초과할 수 없습니다")
    private String name;

    @Size(max = 100, message = "짧은 이름은 100자를 초과할 수 없습니다")
    private String shortName;

    private String description;

    private Long parentId; // 부모 옵션 ID (계층구조인 경우)

    @Builder.Default
    private Integer depth = 0;

    @Size(max = 500, message = "경로는 500자를 초과할 수 없습니다")
    private String path;

    private Map<String, Object> metadata;

    @Builder.Default
    private Integer displayOrder = 0;

    @Size(max = 100, message = "아이콘은 100자를 초과할 수 없습니다")
    private String icon;

    @Size(max = 20, message = "색상은 20자를 초과할 수 없습니다")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^$", message = "올바른 HEX 색상 형식이 아닙니다")
    private String color;

    @NotNull(message = "활성 상태는 필수입니다")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean isExpanded = false;

    public FilterOption toEntity(FilterCategory category, FilterOption parent) {
        FilterOption.FilterOptionBuilder builder = FilterOption.builder()
                .category(category)
                .code(code)
                .name(name)
                .shortName(shortName)
                .description(description)
                .depth(depth)
                .path(path)
                .displayOrder(displayOrder)
                .icon(icon)
                .color(color)
                .isActive(isActive)
                .isDefault(isDefault)
                .isExpanded(isExpanded);

        if (parent != null) {
            builder.parent(parent);
        }

        return builder.build();
    }
}