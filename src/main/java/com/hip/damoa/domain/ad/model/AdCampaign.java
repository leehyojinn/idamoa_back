package com.hip.damoa.domain.ad.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 광고 캠페인 통합 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_campaigns", indexes = {
    @Index(name = "idx_ad_campaigns_company_id", columnList = "company_id"),
    @Index(name = "idx_ad_campaigns_status", columnList = "status"),
    @Index(name = "idx_ad_campaigns_priority_score", columnList = "priority_score")
})
public class AdCampaign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false, length = 30)
    private AdType adType; // LISTING, AI_RECOMMENDATION, BANNER, POPUP

    @Type(JsonBinaryType.class)
    @Column(name = "ad_config", columnDefinition = "jsonb")
    private Map<String, Object> adConfig;

    @Type(JsonBinaryType.class)
    @Column(name = "targeting", columnDefinition = "jsonb")
    private Map<String, Object> targeting;

    @Column(name = "budget_type", length = 20)
    private String budgetType; // DAILY, TOTAL, UNLIMITED

    @Column(name = "budget_amount", precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "daily_budget", precision = 12, scale = 2)
    private BigDecimal dailyBudget;

    @Column(name = "total_spent", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "total_value_30d", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalValue30d = BigDecimal.ZERO;

    @Column(name = "priority_score", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priorityScore = BigDecimal.ZERO;

    @Column(name = "secondary_score", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal secondaryScore = BigDecimal.ZERO;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, ACTIVE, PAUSED, COMPLETED, CANCELLED

    @Column(name = "total_impressions", nullable = false)
    @Builder.Default
    private Long totalImpressions = 0L;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private Long totalClicks = 0L;

    @Column(name = "total_conversions", nullable = false)
    @Builder.Default
    private Long totalConversions = 0L;

    // ===== Business Methods =====

    /**
     * 캠페인 시작
     */
    public void activate() {
        this.status = "ACTIVE";
    }

    /**
     * 캠페인 일시정지
     */
    public void pause() {
        this.status = "PAUSED";
    }

    /**
     * 캠페인 완료
     */
    public void complete() {
        this.status = "COMPLETED";
    }

    /**
     * 캠페인 취소
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * 30일 환산 가치 업데이트
     */
    public void updateValue30d(BigDecimal value) {
        this.totalValue30d = value;
        this.priorityScore = value;
    }

    /**
     * 2차 점수 업데이트
     */
    public void updateSecondaryScore(BigDecimal score) {
        this.secondaryScore = score;
    }

    /**
     * 노출 수 증가
     */
    public void incrementImpressions() {
        this.totalImpressions++;
    }

    /**
     * 클릭 수 증가
     */
    public void incrementClicks() {
        this.totalClicks++;
    }

    /**
     * 전환 수 증가
     */
    public void incrementConversions() {
        this.totalConversions++;
    }

    /**
     * 지출 추가
     */
    public void addSpent(BigDecimal amount) {
        this.totalSpent = this.totalSpent.add(amount);
    }

    /**
     * 프리미엄 설정
     */
    public void setPremium(LocalDateTime until) {
        this.isPremium = true;
        this.premiumUntil = until;
    }

    /**
     * 프리미엄 해제
     */
    public void removePremium() {
        this.isPremium = false;
        this.premiumUntil = null;
    }
}
