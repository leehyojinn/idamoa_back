package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 광고 일별 성과 스냅샷 (V2 추가)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_daily_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_daily_snapshot", columnNames = {"campaign_id", "snapshot_date"})
    },
    indexes = {
        @Index(name = "idx_ad_daily_snapshots_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_ad_daily_snapshots_date", columnList = "snapshot_date")
    })
public class AdDailySnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "value_30d", precision = 12, scale = 2, nullable = false)
    private BigDecimal value30d;

    @Column(name = "daily_rank")
    private Integer dailyRank;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "spent_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "ctr", precision = 5, scale = 2)
    private BigDecimal ctr; // Click Through Rate

    @Column(name = "cpc", precision = 12, scale = 2)
    private BigDecimal cpc; // Cost Per Click

    @Column(name = "cvr", precision = 5, scale = 2)
    private BigDecimal cvr; // Conversion Rate

    /**
     * CTR 계산 (클릭률)
     */
    public void calculateCtr() {
        if (impressions > 0) {
            this.ctr = BigDecimal.valueOf(clicks)
                .divide(BigDecimal.valueOf(impressions), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * CPC 계산 (클릭당 비용)
     */
    public void calculateCpc() {
        if (clicks > 0) {
            this.cpc = spentAmount.divide(BigDecimal.valueOf(clicks), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * CVR 계산 (전환율)
     */
    public void calculateCvr() {
        if (clicks > 0) {
            this.cvr = BigDecimal.valueOf(conversions)
                .divide(BigDecimal.valueOf(clicks), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
}
