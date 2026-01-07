package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryCreateRequest {

    @NotBlank(message = "카테고리명은 필수입니다")
    @Size(max = 100, message = "카테고리명은 100자를 초과할 수 없습니다")
    private String name;

    @NotBlank(message = "슬러그는 필수입니다")
    @Size(max = 100, message = "슬러그는 100자를 초과할 수 없습니다")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "슬러그는 영문 소문자, 숫자, 하이픈만 사용 가능합니다")
    private String slug;

    private String description;

    @Size(max = 100)
    private String icon;

    private Integer displayOrder;

    private Boolean isActive;

    private Boolean allowAnonymous;

    private Boolean requireLogin;

    private Boolean allowAttachments;

    private Integer maxAttachments;
}
