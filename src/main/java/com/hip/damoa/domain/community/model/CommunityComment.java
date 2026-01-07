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
 * 커뮤니티 댓글 엔티티 (무한 대댓글 지원)
 */
@Entity
@Table(name = "community_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommunityComment parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    private Integer dislikeCount = 0;

    @Column(nullable = false)
    private Integer depth = 0;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<CommunityComment> children = new ArrayList<>();

    @Builder
    public CommunityComment(CommunityPost post, CommunityComment parent, User user,
                            String content, Boolean isAnonymous, String ipAddress) {
        this.uuid = UUID.randomUUID();
        this.post = post;
        this.parent = parent;
        this.user = user;
        this.content = content;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.ipAddress = ipAddress;
        this.depth = parent != null ? parent.getDepth() + 1 : 0;
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    public void update(String content) {
        if (content != null) this.content = content;
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

    public void hide() {
        this.isHidden = true;
    }

    public void show() {
        this.isHidden = false;
    }
}
