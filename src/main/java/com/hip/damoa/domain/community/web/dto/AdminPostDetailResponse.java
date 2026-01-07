package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityPost;
import com.hip.damoa.domain.community.model.ContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자용 게시글 상세 - 실제 작성자 정보 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPostDetailResponse {

    private UUID uuid;
    private String title;
    private String content;
    private ContentType contentType;

    private UUID categoryUuid;
    private String categoryName;
    private String categorySlug;

    // 관리자는 실제 작성자 정보 확인 가능
    private Long userId;
    private String userEmail;
    private String realAuthorName;
    private Boolean isAnonymous;

    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;

    private Boolean isPinned;
    private Boolean isNotice;
    private Boolean isPublished;
    private Boolean isDeleted;

    private String ipAddress;

    private List<CommunityFileResponse> attachments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static AdminPostDetailResponse from(CommunityPost post, String realAuthorName,
                                                List<CommunityFileResponse> attachments) {
        return AdminPostDetailResponse.builder()
                .uuid(post.getUuid())
                .title(post.getTitle())
                .content(post.getContent())
                .contentType(post.getContentType())
                .categoryUuid(post.getCategory().getUuid())
                .categoryName(post.getCategory().getName())
                .categorySlug(post.getCategory().getSlug())
                .userId(post.getUser().getId())
                .userEmail(post.getUser().getEmail())
                .realAuthorName(realAuthorName)
                .isAnonymous(post.getIsAnonymous())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .isNotice(post.getIsNotice())
                .isPublished(post.getIsPublished())
                .isDeleted(post.getIsDeleted())
                .ipAddress(post.getIpAddress())
                .attachments(attachments)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deletedAt(post.getDeletedAt())
                .build();
    }
}
