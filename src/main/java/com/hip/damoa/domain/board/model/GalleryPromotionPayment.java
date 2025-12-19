package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 갤러리 우대 등록 결제 내역
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "gallery_promotion_payments", indexes = {
    @Index(name = "idx_gallery_promotion_payments_promotion", columnList = "promotion_id")
})
public class GalleryPromotionPayment extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private GalleryPromotion promotion;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "apply_from_date", nullable = false)
    private LocalDate applyFromDate;

    @Column(name = "apply_to_date", nullable = false)
    private LocalDate applyToDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    @Builder.Default
    private PaymentType paymentType = PaymentType.INITIAL;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "credit_transaction_id")
    private Long creditTransactionId;

    /**
     * 결제 유형
     */
    public enum PaymentType {
        INITIAL,    // 최초 결제
        RENEWAL,    // 자동 갱신
        UPGRADE     // 업그레이드 (STANDARD → PREMIUM)
    }

    /**
     * 정적 팩토리 메서드 - 최초 결제
     */
    public static GalleryPromotionPayment createInitial(GalleryPromotion promotion, Long creditTransactionId) {
        return GalleryPromotionPayment.builder()
                .promotion(promotion)
                .paymentAmount(promotion.getMonthlyPrice())
                .paymentDate(LocalDate.now())
                .applyFromDate(promotion.getStartDate())
                .applyToDate(promotion.getEndDate())
                .paymentType(PaymentType.INITIAL)
                .status("COMPLETED")
                .creditTransactionId(creditTransactionId)
                .build();
    }

    /**
     * 정적 팩토리 메서드 - 자동 갱신
     */
    public static GalleryPromotionPayment createRenewal(GalleryPromotion promotion, LocalDate newEndDate, Long creditTransactionId) {
        return GalleryPromotionPayment.builder()
                .promotion(promotion)
                .paymentAmount(promotion.getMonthlyPrice())
                .paymentDate(LocalDate.now())
                .applyFromDate(promotion.getEndDate().plusDays(1))
                .applyToDate(newEndDate)
                .paymentType(PaymentType.RENEWAL)
                .status("COMPLETED")
                .creditTransactionId(creditTransactionId)
                .build();
    }

    /**
     * 정적 팩토리 메서드 - 업그레이드
     */
    public static GalleryPromotionPayment createUpgrade(GalleryPromotion promotion, BigDecimal upgradePrice, Long creditTransactionId) {
        return GalleryPromotionPayment.builder()
                .promotion(promotion)
                .paymentAmount(upgradePrice)
                .paymentDate(LocalDate.now())
                .applyFromDate(LocalDate.now())
                .applyToDate(promotion.getEndDate())
                .paymentType(PaymentType.UPGRADE)
                .status("COMPLETED")
                .creditTransactionId(creditTransactionId)
                .build();
    }
}
