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

    /**
     * 총 1일 가치 (모든 활성 결제의 dailyValue 합산)
     * 예: 결제1(100원/일) + 결제2(100원/일) = 200원/일
     */
    @Column(name = "total_daily_value", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDailyValue = BigDecimal.ZERO;

    /**
     * 우선순위 점수 (= totalDailyValue)
     * 이 값이 높을수록 상위 노출
     */
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

    @Column(name = "duration_days")
    @Builder.Default
    private Integer durationDays = 7;

    @Column(name = "min_daily_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minDailyAmount = new BigDecimal("500");

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    /**
     * 현재 사이클 누적 결제 금액 (초기 결제 + 추가 결제)
     * 자동 갱신 시 이 금액으로 결제
     */
    @Column(name = "accumulated_payment", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal accumulatedPayment = BigDecimal.ZERO;

    /**
     * 현재 사이클 시작일 (갱신 시 업데이트)
     */
    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    /**
     * 갱신 알림 발송 시간 (3일 전 알림)
     */
    @Column(name = "renewal_notified_at")
    private LocalDateTime renewalNotifiedAt;

    /**
     * 현재 사이클에서 갱신 알림 발송 여부
     */
    @Column(name = "renewal_notified", nullable = false)
    @Builder.Default
    private Boolean renewalNotified = false;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

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
     * 1일 가치 업데이트 (우선순위 점수 = 1일 가치)
     * 예: 총 dailyValue가 200원이면 priorityScore도 200
     */
    public void updateDailyValue(BigDecimal dailyValue) {
        this.totalDailyValue = dailyValue;
        this.priorityScore = dailyValue;
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

    /**
     * 종료일 설정
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * 종료일 설정 (갱신 시 사용)
     */
    public void updateEndDate(LocalDate newEndDate) {
        this.endDate = newEndDate;
    }

    /**
     * 마지막 우선순위 계산 시간 업데이트
     */
    public void updateLastCalculatedAt() {
        this.lastCalculatedAt = LocalDateTime.now();
    }

    /**
     * 광고 기간 및 최소 금액 설정
     */
    public void setDurationConfig(Integer durationDays, BigDecimal minDailyAmount) {
        this.durationDays = durationDays;
        this.minDailyAmount = minDailyAmount;
    }

    /**
     * 자동 갱신 설정
     */
    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    /**
     * 남은 일수 계산
     */
    public int getRemainingDays() {
        if (this.endDate == null) {
            return Integer.MAX_VALUE;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(this.endDate)) {
            return 0;
        }
        return (int) (this.endDate.toEpochDay() - today.toEpochDay()) + 1;
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * 만료되었는지 확인
     */
    public boolean isExpired() {
        if (this.endDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.endDate);
    }

    // ===== 자동 갱신 관련 메서드 =====

    /**
     * 누적 결제 금액 추가
     */
    public void addAccumulatedPayment(BigDecimal amount) {
        if (this.accumulatedPayment == null) {
            this.accumulatedPayment = BigDecimal.ZERO;
        }
        this.accumulatedPayment = this.accumulatedPayment.add(amount);
    }

    /**
     * 자동 갱신을 위한 새 사이클 시작
     * - 새 시작일/종료일 설정
     * - 이전 사이클 누적 금액으로 결제
     * - 갱신 알림 상태 초기화
     */
    public void startNewCycle(LocalDate newStartDate, LocalDate newEndDate, BigDecimal paymentAmount) {
        this.cycleStartDate = newStartDate;
        this.startDate = newStartDate;
        this.endDate = newEndDate;
        this.accumulatedPayment = paymentAmount;  // 새 사이클 시작 금액
        this.renewalNotified = false;
        this.renewalNotifiedAt = null;
    }

    /**
     * 갱신 알림 발송 완료 처리
     */
    public void markRenewalNotified() {
        this.renewalNotified = true;
        this.renewalNotifiedAt = LocalDateTime.now();
    }

    /**
     * 갱신 알림 필요 여부 확인
     * - 자동 갱신 활성화
     * - 활성 캠페인
     * - 아직 알림 발송 안함
     * - 종료 3일 전
     */
    public boolean needsRenewalNotification() {
        if (!Boolean.TRUE.equals(this.autoRenew) || !isActive()) {
            return false;
        }
        if (Boolean.TRUE.equals(this.renewalNotified)) {
            return false;
        }
        if (this.endDate == null) {
            return false;
        }
        LocalDate notificationDate = this.endDate.minusDays(3);
        return !LocalDate.now().isBefore(notificationDate);
    }

    /**
     * 자동 갱신 대상 여부 확인
     * - 자동 갱신 활성화
     * - 활성 캠페인
     * - 오늘이 종료일
     */
    public boolean isEligibleForAutoRenewal() {
        if (!Boolean.TRUE.equals(this.autoRenew) || !isActive()) {
            return false;
        }
        if (this.endDate == null) {
            return false;
        }
        return LocalDate.now().equals(this.endDate);
    }

    /**
     * 갱신 금액 조회 (누적 결제 금액)
     */
    public BigDecimal getRenewalAmount() {
        return this.accumulatedPayment != null ? this.accumulatedPayment : BigDecimal.ZERO;
    }

    /**
     * 사이클 시작일 초기화 (최초 생성 시)
     */
    public void initializeCycle(LocalDate startDate, BigDecimal initialPayment) {
        this.cycleStartDate = startDate;
        this.accumulatedPayment = initialPayment;
    }
}
