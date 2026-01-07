package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostListResponse {

    private UUID uuid;
    private String title;
    private String categoryName;
    private String categorySlug;
    private String authorName;
    private Boolean isAnonymous;
    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private Boolean isNotice;
    private Boolean hasAttachments;
    private LocalDateTime createdAt;

    private Boolean isHiddenByAdmin;

    // 로그인 사용자용
    private Boolean isLiked;
    private Boolean isDisliked;
    private Boolean isOwner;

    private static final String HIDDEN_BY_ADMIN_MESSAGE = "관리자에 의해 비공개 되었습니다";

    public static CommunityPostListResponse from(CommunityPost post, String authorName,
                                                  Boolean isLiked, Boolean isDisliked, Boolean isOwner) {
        boolean isHidden = !post.getIsPublished();
        String displayTitle = isHidden ? HIDDEN_BY_ADMIN_MESSAGE : post.getTitle();

        return CommunityPostListResponse.builder()
                .uuid(post.getUuid())
                .title(displayTitle)
                .categoryName(post.getCategory().getName())
                .categorySlug(post.getCategory().getSlug())
                .authorName(post.getIsAnonymous() ? "익명" : authorName)
                .isAnonymous(post.getIsAnonymous())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .isNotice(post.getIsNotice())
                .hasAttachments(post.getAttachments() != null && !post.getAttachments().isEmpty())
                .createdAt(post.getCreatedAt())
                .isHiddenByAdmin(isHidden)
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isOwner(isOwner)
                .build();
    }
}
