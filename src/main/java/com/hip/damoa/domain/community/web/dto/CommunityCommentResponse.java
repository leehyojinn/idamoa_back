package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.community.model.CommunityComment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentResponse {

    private UUID uuid;
    private String content;
    private String authorName;
    private Boolean isAnonymous;
    private Integer depth;
    private Integer likeCount;
    private Integer dislikeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private Boolean isHiddenByAdmin;

    // 로그인 사용자용
    private Boolean isLiked;
    private Boolean isDisliked;
    private Boolean isOwner;

    // 대댓글
    @Builder.Default
    private List<CommunityCommentResponse> children = new ArrayList<>();

    private static final String DELETED_MESSAGE = "삭제된 댓글입니다";
    private static final String HIDDEN_BY_ADMIN_MESSAGE = "관리자에 의해 비공개 되었습니다";

    public static CommunityCommentResponse from(CommunityComment comment, String authorName,
                                                 Boolean isLiked, Boolean isDisliked, Boolean isOwner) {
        String displayContent = comment.getContent();
        if (comment.getIsDeleted()) {
            displayContent = DELETED_MESSAGE;
        } else if (comment.getIsHidden()) {
            displayContent = HIDDEN_BY_ADMIN_MESSAGE;
        }

        return CommunityCommentResponse.builder()
                .uuid(comment.getUuid())
                .content(displayContent)
                .authorName(comment.getIsAnonymous() ? "익명" : authorName)
                .isAnonymous(comment.getIsAnonymous())
                .depth(comment.getDepth())
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.getIsDeleted())
                .isHiddenByAdmin(comment.getIsHidden())
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isOwner(isOwner)
                .children(new ArrayList<>())
                .build();
    }

    public void setChildren(List<CommunityCommentResponse> children) {
        this.children = children;
    }
}
