package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 광고 클릭 기록
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_clicks", indexes = {
    @Index(name = "idx_ad_clicks_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_clicks_user_id", columnList = "user_id"),
    @Index(name = "idx_ad_clicks_created_at", columnList = "created_at")
})
public class AdClick extends BaseTimeEntity {

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

    @Column(name = "click_time", nullable = false)
    @Builder.Default
    private LocalDateTime clickTime = LocalDateTime.now();

    @Column(name = "landing_url", length = 500)
    private String landingUrl;

    @Column(name = "referrer_url", length = 500)
    private String referrerUrl;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "converted", nullable = false)
    @Builder.Default
    private Boolean converted = false;

    @Column(name = "conversion_time")
    private LocalDateTime conversionTime;

    @Column(name = "conversion_value", precision = 12, scale = 2)
    private java.math.BigDecimal conversionValue;

    // ===== Business Methods =====

    /**
     * 전환 처리
     */
    public void markAsConverted(java.math.BigDecimal value) {
        this.converted = true;
        this.conversionTime = LocalDateTime.now();
        this.conversionValue = value;
    }
}
