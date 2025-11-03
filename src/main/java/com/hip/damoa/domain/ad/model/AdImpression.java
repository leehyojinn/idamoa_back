package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 광고 노출 기록
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_impressions", indexes = {
    @Index(name = "idx_ad_impressions_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_impressions_user_id", columnList = "user_id"),
    @Index(name = "idx_ad_impressions_created_at", columnList = "created_at")
})
public class AdImpression extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creative_id")
    private AdCreative creative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "impression_time", nullable = false)
    @Builder.Default
    private LocalDateTime impressionTime = LocalDateTime.now();

    @Column(name = "placement", length = 100)
    private String placement; // MAIN_TOP, SIDEBAR, LISTING_TOP 등

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "referrer_url", length = 500)
    private String referrerUrl;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 20)
    private String deviceType; // DESKTOP, MOBILE, TABLET

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "session_id", length = 100)
    private String sessionId;
}
