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
    @Index(name = "idx_board_categories_type", columnList = "board_type"),
    @Index(name = "idx_board_categories_slug", columnList = "slug")
})
public class BoardCategory extends BaseEntity {

    @Column(name = "board_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", length = 100)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BoardCategory parent;

    @Column(name = "depth")
    @Builder.Default
    private Integer depth = 0;

    @Column(name = "path", length = 500)
    private String path;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
