package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 업체 리뷰
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_reviews", indexes = {
    @Index(name = "idx_company_reviews_company_id", columnList = "company_id"),
    @Index(name = "idx_company_reviews_user_id", columnList = "user_id"),
    @Index(name = "idx_company_reviews_rating", columnList = "rating")
})
public class CompanyReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating; // 1.0 ~ 5.0

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Type(LongArrayType.class)
    @Column(name = "images", columnDefinition = "bigint[]")
    private Long[] images;  // File ID 배열 (Deprecated - use reviewImages instead)

    /**
     * 리뷰 이미지 목록 (OneToMany relationship via join table)
     * Company 패턴과 동일: File ID 기반 관리
     */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CompanyReviewImage> reviewImages = new ArrayList<>();

    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PUBLISHED"; // PUBLISHED, HIDDEN, REPORTED, DELETED

    // ===== Business Methods =====

    /**
     * 답변 작성
     */
    public void addReply(String reply) {
        this.reply = reply;
        this.repliedAt = LocalDateTime.now();
    }

    /**
     * 답변 수정
     */
    public void updateReply(String reply) {
        this.reply = reply;
    }

    /**
     * 답변 삭제
     */
    public void deleteReply() {
        this.reply = null;
        this.repliedAt = null;
    }

    /**
     * 리뷰 수정
     */
    public void updateReview(BigDecimal rating, String title, String content, Long[] images) {
        this.rating = rating;
        this.title = title;
        this.content = content;
        this.images = images;
    }

    /**
     * 리뷰 이미지 업데이트 (OneToMany 기반)
     */
    public void updateReviewImages(List<CompanyReviewImage> newImages) {
        this.reviewImages.clear();
        if (newImages != null) {
            this.reviewImages.addAll(newImages);
        }
    }

    /**
     * 리뷰 이미지 추가
     */
    public void addReviewImage(CompanyReviewImage image) {
        this.reviewImages.add(image);
    }

    /**
     * 리뷰 삭제 (Soft Delete)
     */
    public void softDelete() {
        this.status = "DELETED";
    }

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
     * 신고 증가
     */
    public void incrementReportCount() {
        this.reportCount++;
        if (this.reportCount >= 5) {
            this.status = "REPORTED";
        }
    }

    /**
     * 상태 변경
     */
    public void changeStatus(String status) {
        this.status = status;
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
}
