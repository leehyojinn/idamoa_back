package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCategoryResponse {

    private UUID uuid;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean allowAnonymous;
    private Boolean requireLogin;
    private Boolean allowAttachments;
    private Integer maxAttachments;
    private LocalDateTime createdAt;

    public static CommunityCategoryResponse from(CommunityCategory category) {
        return CommunityCategoryResponse.builder()
                .uuid(category.getUuid())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .allowAnonymous(category.getAllowAnonymous())
                .requireLogin(category.getRequireLogin())
                .allowAttachments(category.getAllowAttachments())
                .maxAttachments(category.getMaxAttachments())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
