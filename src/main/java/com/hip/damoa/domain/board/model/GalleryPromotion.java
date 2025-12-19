package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 갤러리(포트폴리오) 우대 등록
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "gallery_promotions", indexes = {
    @Index(name = "idx_gallery_promotions_status", columnList = "status"),
    @Index(name = "idx_gallery_promotions_end_date", columnList = "end_date"),
    @Index(name = "idx_gallery_promotions_user_id", columnList = "user_id")
})
public class GalleryPromotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 20)
    private GalleryPromotionType promotionType;

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

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    // ===== Business Methods =====

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
        return LocalDate.now().isAfter(this.endDate);
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.status = "EXPIRED";
    }

    /**
     * 취소 처리 (자동갱신 OFF, 현재 기간 유지)
     */
    public void cancel() {
        this.autoRenew = false;
    }

    /**
     * 자동갱신 설정 변경
     */
    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    /**
     * 갱신 알림 발송 완료 처리
     */
    public void markRenewalNotified() {
        this.renewalNotified = true;
    }

    /**
     * 기간 연장 (자동 갱신 시)
     */
    public void extendPeriod(LocalDate newEndDate) {
        this.startDate = this.endDate.plusDays(1);
        this.endDate = newEndDate;
        this.renewalNotified = false;
    }

    /**
     * PREMIUM으로 업그레이드
     */
    public void upgradeToPremium() {
        this.promotionType = GalleryPromotionType.PREMIUM;
        this.weight = GalleryPromotionType.PREMIUM.getWeight();
        this.monthlyPrice = GalleryPromotionType.PREMIUM.getMonthlyPrice();
    }

    /**
     * 자동 갱신 대상 여부 확인
     * - 자동 갱신 활성화
     * - 활성 상태
     * - 오늘이 종료일
     */
    public boolean isEligibleForAutoRenewal() {
        if (!Boolean.TRUE.equals(this.autoRenew) || !isActive()) {
            return false;
        }
        return LocalDate.now().equals(this.endDate);
    }

    /**
     * 갱신 알림 필요 여부 확인
     * - 자동 갱신 활성화
     * - 활성 상태
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
        LocalDate notificationDate = this.endDate.minusDays(3);
        return !LocalDate.now().isBefore(notificationDate);
    }

    /**
     * 남은 일수 계산
     */
    public int getRemainingDays() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(this.endDate)) {
            return 0;
        }
        return (int) (this.endDate.toEpochDay() - today.toEpochDay()) + 1;
    }

    /**
     * 정적 팩토리 메서드 (enum 기본값 사용)
     * @deprecated Use {@link #create(Board, User, GalleryPromotionType, Boolean, BigDecimal, Integer)} instead
     */
    @Deprecated
    public static GalleryPromotion create(Board board, User user, GalleryPromotionType type, Boolean autoRenew) {
        return create(board, user, type, autoRenew, type.getMonthlyPrice(), type.getWeight());
    }

    /**
     * 정적 팩토리 메서드 (동적 가격/가중치 사용)
     */
    public static GalleryPromotion create(Board board, User user, GalleryPromotionType type, Boolean autoRenew,
                                          BigDecimal monthlyPrice, Integer weight) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(1).minusDays(1);

        return GalleryPromotion.builder()
                .board(board)
                .user(user)
                .promotionType(type)
                .weight(weight)
                .monthlyPrice(monthlyPrice)
                .startDate(today)
                .endDate(endDate)
                .autoRenew(autoRenew != null ? autoRenew : false)
                .renewalNotified(false)
                .status("ACTIVE")
                .build();
    }

    /**
     * PREMIUM으로 업그레이드 (동적 가격/가중치 사용)
     */
    public void upgradeToPremium(BigDecimal newPrice, Integer newWeight) {
        this.promotionType = GalleryPromotionType.PREMIUM;
        this.weight = newWeight;
        this.monthlyPrice = newPrice;
    }
}
