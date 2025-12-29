package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * 필터 옵션 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionUpdateRequest {

    @Size(max = 200, message = "이름은 200자를 초과할 수 없습니다")
    private String name;

    @Size(max = 100, message = "짧은 이름은 100자를 초과할 수 없습니다")
    private String shortName;

    private String description;

    @Size(max = 500, message = "경로는 500자를 초과할 수 없습니다")
    private String path;

    private Map<String, Object> metadata;

    private Integer displayOrder;

    @Size(max = 100, message = "아이콘은 100자를 초과할 수 없습니다")
    private String icon;

    @Size(max = 20, message = "색상은 20자를 초과할 수 없습니다")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^$", message = "올바른 HEX 색상 형식이 아닙니다")
    private String color;

    private Boolean isActive;

    private Boolean isDefault;

    private Boolean isExpanded;
}