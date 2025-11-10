package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 업체 기본 정보 통합 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "companies", indexes = {
    @Index(name = "idx_companies_owner_id", columnList = "owner_id"),
    @Index(name = "idx_companies_status", columnList = "status"),
    @Index(name = "idx_companies_avg_rating", columnList = "avg_rating")
})
public class Company extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", unique = true, length = 200)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "detail_content", columnDefinition = "TEXT")
    private String detailContent;

    @Column(name = "detail_content_format", length = 20)
    @Builder.Default
    private String detailContentFormat = "HTML"; // HTML, MARKDOWN

    @Type(JsonBinaryType.class)
    @Column(name = "business_info", columnDefinition = "jsonb")
    private Map<String, Object> businessInfo;

    @Type(JsonBinaryType.class)
    @Column(name = "business_hours", columnDefinition = "jsonb")
    private Map<String, Object> businessHours;

    @Column(name = "business_hours_note", columnDefinition = "TEXT")
    private String businessHoursNote;

    @Type(StringArrayType.class)
    @Column(name = "service_areas", columnDefinition = "text[]")
    private String[] serviceAreas;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Type(StringArrayType.class)
    @Column(name = "keywords", columnDefinition = "text[]")
    private String[] keywords;

    @Column(name = "primary_phone", length = 20)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "kakao_chat_url", length = 500)
    private String kakaoChatUrl;

    @Type(JsonBinaryType.class)
    @Column(name = "social_links", columnDefinition = "jsonb")
    private Map<String, Object> socialLinks;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "avg_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "portfolio_count", nullable = false)
    @Builder.Default
    private Integer portfolioCount = 0;

    @Column(name = "completed_projects", nullable = false)
    @Builder.Default
    private Integer completedProjects = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED, PENDING

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "premium_tier", nullable = false, length = 20)
    @Builder.Default
    private String premiumTier = "NONE"; // NONE, BASIC, STANDARD, PREMIUM, VIP

    @Column(name = "premium_monthly_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal premiumMonthlyAmount = BigDecimal.ZERO;

    @Type(LongArrayType.class)
    @Column(name = "images", columnDefinition = "bigint[]")
    private Long[] images;  // File ID 배열

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CompanyFilterOption> filterOptions = new ArrayList<>();

    // ===== Business Methods =====

    /**
     * 업체 정보 업데이트
     */
    public void updateBasicInfo(String name, String description, String detailContent) {
        this.name = name;
        this.description = description;
        this.detailContent = detailContent;
    }

    /**
     * 연락처 정보 업데이트
     */
    public void updateContact(String primaryPhone, String email, String websiteUrl) {
        this.primaryPhone = primaryPhone;
        this.email = email;
        this.websiteUrl = websiteUrl;
    }

    /**
     * 주소 업데이트
     */
    public void updateAddress(String address, String postalCode, BigDecimal latitude, BigDecimal longitude) {
        this.address = address;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 인증 처리
     */
    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

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
     * 평점 업데이트
     */
    public void updateRating(BigDecimal newAvgRating, Integer newReviewCount) {
        this.avgRating = newAvgRating;
        this.reviewCount = newReviewCount;
    }

    /**
     * 프리미엄 설정
     */
    public void setPremium(LocalDateTime until) {
        this.premiumUntil = until;
    }

    /**
     * 프리미엄 여부 확인
     */
    public boolean isPremium() {
        return premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now());
    }

    /**
     * 상태 변경
     */
    public void changeStatus(String status) {
        this.status = status;
    }

    /**
     * 추천 업체 설정
     */
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * 프리미엄 등급 설정
     */
    public void setPremiumTier(String premiumTier, BigDecimal monthlyAmount) {
        this.premiumTier = premiumTier;
        this.premiumMonthlyAmount = monthlyAmount;
    }

    /**
     * 프리미엄 등급 확인
     */
    public boolean hasPremiumTier() {
        return !"NONE".equals(this.premiumTier);
    }

    /**
     * 이미지 업데이트
     */
    public void updateImages(Long[] images) {
        this.images = images;
    }

    /**
     * 필터 옵션 업데이트 (기존 것을 모두 제거하고 새로 추가)
     */
    public void updateFilterOptions(List<CompanyFilterOption> newFilterOptions) {
        this.filterOptions.clear();
        if (newFilterOptions != null) {
            this.filterOptions.addAll(newFilterOptions);
        }
    }

    /**
     * 필터 옵션 추가
     */
    public void addFilterOption(CompanyFilterOption filterOption) {
        this.filterOptions.add(filterOption);
    }
}
