package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 광고 소재 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_creatives", indexes = {
    @Index(name = "idx_ad_creatives_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_creatives_status", columnList = "status")
})
public class AdCreative extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "landing_url", length = 500)
    private String landingUrl;

    @Column(name = "creative_type", length = 50)
    private String creativeType; // IMAGE, VIDEO, HTML5, CAROUSEL

    @Column(name = "creative_size", length = 50)
    private String creativeSize; // 728x90, 300x250, etc.

    @Column(name = "creative_format", length = 20)
    private String creativeFormat; // JPG, PNG, GIF, MP4, etc.

    @Column(name = "ab_test_group", length = 20)
    private String abTestGroup; // A, B, C, CONTROL

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, ACTIVE, PAUSED, ARCHIVED

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    // ===== Business Methods =====

    /**
     * 소재 활성화
     */
    public void activate() {
        this.status = "ACTIVE";
    }

    /**
     * 소재 일시정지
     */
    public void pause() {
        this.status = "PAUSED";
    }

    /**
     * 소재 보관
     */
    public void archive() {
        this.status = "ARCHIVED";
    }

    /**
     * 노출 수 증가
     */
    public void incrementImpressions() {
        this.impressions++;
    }

    /**
     * 클릭 수 증가
     */
    public void incrementClicks() {
        this.clicks++;
    }

    /**
     * 전환 수 증가
     */
    public void incrementConversions() {
        this.conversions++;
    }

    /**
     * CTR 계산
     */
    public double getCtr() {
        if (impressions == 0) return 0.0;
        return (double) clicks / impressions * 100;
    }

    /**
     * CVR 계산
     */
    public double getCvr() {
        if (clicks == 0) return 0.0;
        return (double) conversions / clicks * 100;
    }
}
