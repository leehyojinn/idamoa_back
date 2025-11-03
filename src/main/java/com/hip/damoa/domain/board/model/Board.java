package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

/**
 * 게시판
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "boards", indexes = {
    @Index(name = "idx_boards_code", columnList = "board_code"),
    @Index(name = "idx_boards_type", columnList = "board_type")
})
public class Board extends BaseEntity {

    @Column(name = "board_code", unique = true, nullable = false, length = 50)
    private String boardCode;

    @Column(name = "board_name", nullable = false, length = 100)
    private String boardName;

    @Column(name = "board_type", nullable = false, length = 20)
    private String boardType; // NOTICE, FAQ, QNA, COMMUNITY, REVIEW

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(StringArrayType.class)
    @Column(name = "allowed_roles", columnDefinition = "text[]")
    private String[] allowedRoles;

    @Column(name = "is_comment_enabled", nullable = false)
    @Builder.Default
    private Boolean isCommentEnabled = true;

    @Column(name = "is_attachment_enabled", nullable = false)
    @Builder.Default
    private Boolean isAttachmentEnabled = true;

    @Column(name = "is_anonymous_enabled", nullable = false)
    @Builder.Default
    private Boolean isAnonymousEnabled = false;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Long postCount = 0L;

    public void incrementPostCount() {
        this.postCount++;
    }

    public void decrementPostCount() {
        if (this.postCount > 0) {
            this.postCount--;
        }
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }
}
