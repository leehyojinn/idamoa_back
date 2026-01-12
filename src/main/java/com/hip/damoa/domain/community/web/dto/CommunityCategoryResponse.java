package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // 계층 구조 관련 필드
    private Integer depth;
    private ParentInfo parent;
    private List<CommunityCategoryResponse> children;

    /**
     * 부모 카테고리 간략 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentInfo {
        private UUID uuid;
        private String name;
        private String slug;
    }

    /**
     * 기본 변환 (자식 카테고리 미포함)
     */
    public static CommunityCategoryResponse from(CommunityCategory category) {
        return from(category, false, false);
    }

    /**
     * 변환 (자식 카테고리 포함 여부 선택) - 공개용 (활성화된 것만)
     */
    public static CommunityCategoryResponse from(CommunityCategory category, boolean includeChildren) {
        return from(category, includeChildren, false);
    }

    /**
     * 변환 (자식 카테고리 포함 여부 + 관리자 여부 선택)
     * @param includeChildren 자식 카테고리 포함 여부
     * @param forAdmin 관리자용 여부 (true: 비활성 포함, false: 활성만)
     */
    public static CommunityCategoryResponse from(CommunityCategory category, boolean includeChildren, boolean forAdmin) {
        CommunityCategoryResponseBuilder builder = CommunityCategoryResponse.builder()
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
                .depth(category.getDepth());

        // 부모 정보
        if (category.getParent() != null) {
            builder.parent(ParentInfo.builder()
                    .uuid(category.getParent().getUuid())
                    .name(category.getParent().getName())
                    .slug(category.getParent().getSlug())
                    .build());
        }

        // 자식 카테고리 (재귀적으로)
        if (includeChildren && category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CommunityCategoryResponse> childResponses = category.getChildren().stream()
                    .filter(c -> !c.getIsDeleted())  // 삭제된 건 항상 제외
                    .filter(c -> forAdmin || c.getIsActive())  // 관리자가 아니면 활성만
                    .map(c -> CommunityCategoryResponse.from(c, true, forAdmin))
                    .collect(Collectors.toList());
            builder.children(childResponses);
        }

        return builder.build();
    }
}
