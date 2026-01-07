package com.hip.damoa.domain.community.model;

import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 커뮤니티 좋아요/싫어요 엔티티
 */
@Entity
@Table(name = "community_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private CommunityComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "like_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private LikeType likeType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 게시글 좋아요/싫어요
    public static CommunityLike forPost(CommunityPost post, User user, LikeType likeType) {
        CommunityLike like = new CommunityLike();
        like.post = post;
        like.user = user;
        like.likeType = likeType;
        return like;
    }

    // 댓글 좋아요/싫어요
    public static CommunityLike forComment(CommunityComment comment, User user, LikeType likeType) {
        CommunityLike like = new CommunityLike();
        like.comment = comment;
        like.user = user;
        like.likeType = likeType;
        return like;
    }

    public void changeLikeType(LikeType likeType) {
        this.likeType = likeType;
    }
}
