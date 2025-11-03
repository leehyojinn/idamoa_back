package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 업체 포트폴리오
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_portfolios", indexes = {
    @Index(name = "idx_company_portfolios_company_id", columnList = "company_id"),
    @Index(name = "idx_company_portfolios_status", columnList = "status")
})
public class CompanyPortfolio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "area_sqft", precision = 10, scale = 2)
    private BigDecimal areaSqft;

    @Column(name = "budget_range", length = 50)
    private String budgetRange;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "before_image_url", length = 500)
    private String beforeImageUrl;

    @Column(name = "after_image_url", length = 500)
    private String afterImageUrl;

    @Type(StringArrayType.class)
    @Column(name = "images", columnDefinition = "text[]")
    private String[] images;

    @Type(JsonBinaryType.class)
    @Column(name = "project_details", columnDefinition = "jsonb")
    private Map<String, Object> projectDetails;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    // ===== Business Methods =====

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
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
     * 상태 변경
     */
    public void changeStatus(String status) {
        this.status = status;
    }

    /**
     * 추천 포트폴리오 설정
     */
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * 포트폴리오 발행
     */
    public void publish() {
        this.status = "PUBLISHED";
    }

    /**
     * 포트폴리오 보관
     */
    public void archive() {
        this.status = "ARCHIVED";
    }
}
