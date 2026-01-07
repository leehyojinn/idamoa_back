package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityPost;
import com.hip.damoa.domain.community.model.ContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostDetailResponse {

    private UUID uuid;
    private String title;
    private String content;
    private ContentType contentType;

    private UUID categoryUuid;
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
    private Boolean isPublished;
    private Boolean isHiddenByAdmin;

    private List<CommunityFileResponse> attachments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 로그인 사용자용
    private Boolean isLiked;
    private Boolean isDisliked;
    private Boolean isOwner;

    private static final String HIDDEN_BY_ADMIN_MESSAGE = "관리자에 의해 비공개 되었습니다";

    public static CommunityPostDetailResponse from(CommunityPost post, String authorName,
                                                    List<CommunityFileResponse> attachments,
                                                    Boolean isLiked, Boolean isDisliked, Boolean isOwner) {
        boolean isHidden = !post.getIsPublished();
        String displayTitle = isHidden ? HIDDEN_BY_ADMIN_MESSAGE : post.getTitle();
        String displayContent = isHidden ? HIDDEN_BY_ADMIN_MESSAGE : post.getContent();

        return CommunityPostDetailResponse.builder()
                .uuid(post.getUuid())
                .title(displayTitle)
                .content(displayContent)
                .contentType(post.getContentType())
                .categoryUuid(post.getCategory().getUuid())
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
                .isPublished(post.getIsPublished())
                .isHiddenByAdmin(isHidden)
                .attachments(isHidden ? List.of() : attachments)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isOwner(isOwner)
                .build();
    }
}
