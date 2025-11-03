package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

/**
 * 매칭 후기 (양방향 리뷰)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "match_reviews", indexes = {
    @Index(name = "idx_match_reviews_match_id", columnList = "match_id"),
    @Index(name = "idx_match_reviews_reviewer_id", columnList = "reviewer_id")
})
public class MatchReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(name = "reviewer_type", nullable = false, length = 20)
    private String reviewerType; // CLIENT, COMPANY

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating; // 1.0 ~ 5.0

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Type(StringArrayType.class)
    @Column(name = "images", columnDefinition = "text[]")
    private String[] images;

    @Column(name = "communication_rating", precision = 2, scale = 1)
    private BigDecimal communicationRating;

    @Column(name = "quality_rating", precision = 2, scale = 1)
    private BigDecimal qualityRating;

    @Column(name = "schedule_rating", precision = 2, scale = 1)
    private BigDecimal scheduleRating;

    @Column(name = "budget_rating", precision = 2, scale = 1)
    private BigDecimal budgetRating;

    @Column(name = "recommend", nullable = false)
    @Builder.Default
    private Boolean recommend = true;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PUBLISHED"; // PUBLISHED, HIDDEN, REPORTED

    // ===== Business Methods =====

    /**
     * 좋아요 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 리뷰 숨기기
     */
    public void hide() {
        this.status = "HIDDEN";
    }

    /**
     * 리뷰 게시
     */
    public void publish() {
        this.status = "PUBLISHED";
    }

    /**
     * 신고 처리
     */
    public void markAsReported() {
        this.status = "REPORTED";
    }
}
