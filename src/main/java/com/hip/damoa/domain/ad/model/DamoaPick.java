package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;

/**
 * 다모아 추천 업체 (V2 추가)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "damoa_picks", indexes = {
    @Index(name = "idx_damoa_picks_company_id", columnList = "company_id"),
    @Index(name = "idx_damoa_picks_pick_type", columnList = "pick_type"),
    @Index(name = "idx_damoa_picks_season", columnList = "season"),
    @Index(name = "idx_damoa_picks_active", columnList = "is_active")
})
public class DamoaPick extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "pick_type", nullable = false, length = 20)
    private PickType pickType; // SPONSORED, OPERATED, PARTNER

    @Column(name = "season", length = 50)
    private String season; // 2024_SPRING, 2024_SUMMER, 2024_FALL, 2024_WINTER

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "main_image_url", length = 500)
    private String mainImageUrl;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Type(StringArrayType.class)
    @Column(name = "images", columnDefinition = "text[]")
    private String[] images;

    @Column(name = "badge_text", length = 50)
    private String badgeText;

    @Column(name = "badge_color", length = 20)
    private String badgeColor;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Integer clickCount = 0;

    // ===== Business Methods =====

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 클릭수 증가
     */
    public void incrementClickCount() {
        this.clickCount++;
    }

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }

    /**
     * 활성 기간 확인
     */
    public boolean isActivePeriod() {
        LocalDate now = LocalDate.now();
        boolean afterStart = startDate == null || !now.isBefore(startDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        return afterStart && beforeEnd && isActive;
    }

    public enum PickType {
        SPONSORED,  // 후원 업체
        OPERATED,   // 직접 운영 업체
        PARTNER     // 파트너 업체 (렌탈, 침구 등)
    }
}
