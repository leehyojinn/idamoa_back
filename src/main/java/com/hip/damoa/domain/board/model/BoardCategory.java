package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시판 카테고리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_categories", indexes = {
    @Index(name = "idx_board_categories_board_id", columnList = "board_id"),
    @Index(name = "idx_board_categories_code", columnList = "category_code")
})
public class BoardCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
