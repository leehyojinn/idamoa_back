package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 포트폴리오 프로모션 (우대등록)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_promotions", indexes = {
    @Index(name = "idx_portfolio_promotions_status", columnList = "status"),
    @Index(name = "idx_portfolio_promotions_end_date", columnList = "end_date"),
    @Index(name = "idx_portfolio_promotions_user_id", columnList = "user_id")
})
public class PortfolioPromotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CompanyPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "promotion_type", nullable = false, length = 30)
    private String promotionType;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    @Column(name = "renewal_notified", nullable = false)
    @Builder.Default
    private Boolean renewalNotified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PortfolioPromotionStatus status = PortfolioPromotionStatus.ACTIVE;

    // ===== Business Methods =====

    /**
     * 프로모션 만료 처리
     */
    public void expire() {
        this.status = PortfolioPromotionStatus.EXPIRED;
    }

    /**
     * 프로모션 취소 처리
     */
    public void cancel() {
        this.status = PortfolioPromotionStatus.CANCELLED;
    }

    /**
     * 자동갱신 설정
     */
    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    /**
     * 갱신 알림 전송 완료 처리
     */
    public void markRenewalNotified() {
        this.renewalNotified = true;
    }

    /**
     * 갱신 (기간 연장)
     */
    public void renew(LocalDate newEndDate, BigDecimal newPrice) {
        this.endDate = newEndDate;
        this.monthlyPrice = newPrice;
        this.renewalNotified = false;
    }

    /**
     * 업그레이드 (타입 변경)
     */
    public void upgrade(String newType, Integer newWeight, LocalDate newEndDate, BigDecimal newPrice) {
        this.promotionType = newType;
        this.weight = newWeight;
        this.endDate = newEndDate;
        this.monthlyPrice = newPrice;
    }

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return this.status == PortfolioPromotionStatus.ACTIVE;
    }

    /**
     * 만료 예정 확인 (7일 이내)
     */
    public boolean isExpiringSoon() {
        return this.endDate.isBefore(LocalDate.now().plusDays(7)) ||
               this.endDate.isEqual(LocalDate.now().plusDays(7));
    }

    /**
     * 관리자용 프로모션 정보 수정
     */
    public void updateByAdmin(String promotionType, Integer weight, LocalDate startDate,
                               LocalDate endDate, BigDecimal monthlyPrice, Boolean autoRenew) {
        if (promotionType != null) {
            this.promotionType = promotionType;
        }
        if (weight != null) {
            this.weight = weight;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (monthlyPrice != null) {
            this.monthlyPrice = monthlyPrice;
        }
        if (autoRenew != null) {
            this.autoRenew = autoRenew;
        }
    }

    /**
     * 상태 변경 (관리자용)
     */
    public void updateStatus(PortfolioPromotionStatus status) {
        this.status = status;
    }
}
