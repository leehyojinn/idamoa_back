package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 업체 포트폴리오
 * - 기존 Gallery Board 기능을 완전 이관
 * - 프로모션, 필터, 북마크/좋아요 기능 지원
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_portfolios", indexes = {
    @Index(name = "idx_company_portfolios_company_id", columnList = "company_id"),
    @Index(name = "idx_company_portfolios_is_public", columnList = "is_public"),
    @Index(name = "idx_company_portfolios_created_at", columnList = "created_at"),
    @Index(name = "idx_company_portfolios_view_count", columnList = "view_count"),
    @Index(name = "idx_company_portfolios_like_count", columnList = "like_count")
})
public class CompanyPortfolio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(name = "project_scale", length = 50)
    private String projectScale;

    @Column(name = "project_duration")
    private Integer projectDuration;

    @Column(name = "project_date")
    private LocalDate projectDate;

    @Column(name = "budget_range", length = 50)
    private String budgetRange;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "related_link", length = 500)
    private String relatedLink;

    @Column(name = "copyright_owner", length = 200)
    private String copyrightOwner;

    @Column(name = "copyright_license", length = 100)
    private String copyrightLicense;

    @Column(name = "copyright_attribution", length = 500)
    private String copyrightAttribution;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    @Builder.Default
    private Integer bookmarkCount = 0;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * 필터 옵션 ID 목록 (JSONB 배열)
     * - 조인 테이블 대신 JSONB 배열로 관리하여 성능 개선
     * - 검색 시 GIN 인덱스 활용
     */
    @Type(JsonBinaryType.class)
    @Column(name = "filter_option_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<Long> filterOptionIds = new ArrayList<>();

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
     * 북마크 증가
     */
    public void incrementBookmarkCount() {
        this.bookmarkCount++;
    }

    /**
     * 북마크 감소
     */
    public void decrementBookmarkCount() {
        if (this.bookmarkCount > 0) {
            this.bookmarkCount--;
        }
    }

    /**
     * 댓글 수 증가
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수 감소
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 공개/비공개 설정
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * 추천 포트폴리오 설정
     */
    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    /**
     * 포트폴리오 수정
     */
    public void update(String title, String description, String content, String category,
                       String projectType, String projectScale, Integer projectDuration,
                       LocalDate projectDate, String budgetRange, BigDecimal actualCost,
                       String thumbnailUrl, String[] tags,
                       String relatedLink, String copyrightOwner, String copyrightLicense,
                       String copyrightAttribution, Boolean isPublic, Integer displayOrder) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (content != null) this.content = content;
        if (category != null) this.category = category;
        if (projectType != null) this.projectType = projectType;
        if (projectScale != null) this.projectScale = projectScale;
        if (projectDuration != null) this.projectDuration = projectDuration;
        if (projectDate != null) this.projectDate = projectDate;
        if (budgetRange != null) this.budgetRange = budgetRange;
        if (actualCost != null) this.actualCost = actualCost;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (tags != null) this.tags = tags;
        if (relatedLink != null) this.relatedLink = relatedLink;
        if (copyrightOwner != null) this.copyrightOwner = copyrightOwner;
        if (copyrightLicense != null) this.copyrightLicense = copyrightLicense;
        if (copyrightAttribution != null) this.copyrightAttribution = copyrightAttribution;
        if (isPublic != null) this.isPublic = isPublic;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    /**
     * 필터 옵션 ID 목록 업데이트 (JSONB 배열)
     */
    public void updateFilterOptionIds(List<Long> ids) {
        this.filterOptionIds = ids != null ? new ArrayList<>(ids) : new ArrayList<>();
    }
}
