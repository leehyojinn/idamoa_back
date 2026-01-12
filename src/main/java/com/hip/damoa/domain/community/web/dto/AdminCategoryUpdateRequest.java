package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryUpdateRequest {

    @Size(max = 100, message = "카테고리명은 100자를 초과할 수 없습니다")
    private String name;

    private String description;

    @Size(max = 100)
    private String icon;

    private Integer displayOrder;

    private Boolean isActive;

    private Boolean allowAnonymous;

    private Boolean requireLogin;

    private Boolean allowAttachments;

    private Integer maxAttachments;

    // 부모 카테고리 UUID (null이면 최상위로 이동)
    private UUID parentUuid;

    // 부모 변경 여부 (parentUuid가 null일 때 최상위로 이동할지 구분)
    private Boolean changeParent;
}
