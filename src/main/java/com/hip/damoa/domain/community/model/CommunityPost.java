package com.hip.damoa.domain.community.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 엔티티
 */
@Entity
@Table(name = "community_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CommunityCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", length = 20)
    @Enumerated(EnumType.STRING)
    private ContentType contentType = ContentType.TEXT;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    private Integer dislikeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Column(name = "is_notice", nullable = false)
    private Boolean isNotice = false;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<CommunityPostAttachment> attachments = new ArrayList<>();

    @Builder
    public CommunityPost(CommunityCategory category, User user, String title, String content,
                         ContentType contentType, Boolean isAnonymous, String ipAddress) {
        this.uuid = UUID.randomUUID();
        this.category = category;
        this.user = user;
        this.title = title;
        this.content = content;
        this.contentType = contentType != null ? contentType : ContentType.TEXT;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.ipAddress = ipAddress;
        this.viewCount = 0;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.isPinned = false;
        this.isNotice = false;
        this.isPublished = true;
    }

    public void update(String title, String content, ContentType contentType, Boolean isAnonymous) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (contentType != null) this.contentType = contentType;
        if (isAnonymous != null) this.isAnonymous = isAnonymous;
    }

    public void updateByAdmin(String title, String content, ContentType contentType,
                               Boolean isPinned, Boolean isNotice, Boolean isPublished) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (contentType != null) this.contentType = contentType;
        if (isPinned != null) this.isPinned = isPinned;
        if (isNotice != null) this.isNotice = isNotice;
        if (isPublished != null) this.isPublished = isPublished;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void incrementDislikeCount() {
        this.dislikeCount++;
    }

    public void decrementDislikeCount() {
        if (this.dislikeCount > 0) this.dislikeCount--;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void setAsNotice() {
        this.isNotice = true;
    }

    public void unsetAsNotice() {
        this.isNotice = false;
    }

    public void publish() {
        this.isPublished = true;
    }

    public void unpublish() {
        this.isPublished = false;
    }
}
