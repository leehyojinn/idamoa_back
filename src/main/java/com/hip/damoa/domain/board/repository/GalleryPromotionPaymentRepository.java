package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.GalleryPromotion;
import com.hip.damoa.domain.board.model.GalleryPromotionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryPromotionPaymentRepository extends JpaRepository<GalleryPromotionPayment, Long> {

    Optional<GalleryPromotionPayment> findByUuid(UUID uuid);

    List<GalleryPromotionPayment> findByPromotion(GalleryPromotion promotion);

    List<GalleryPromotionPayment> findByPromotionOrderByCreatedAtDesc(GalleryPromotion promotion);

    /**
     * 특정 기간의 결제 내역 조회
     */
    @Query("SELECT gpp FROM GalleryPromotionPayment gpp WHERE gpp.paymentDate >= :startDate AND gpp.paymentDate <= :endDate ORDER BY gpp.createdAt DESC")
    List<GalleryPromotionPayment> findByPaymentDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 특정 Promotion의 총 결제 금액
     */
    @Query("SELECT COALESCE(SUM(gpp.paymentAmount), 0) FROM GalleryPromotionPayment gpp WHERE gpp.promotion = :promotion")
    java.math.BigDecimal getTotalPaymentAmount(@Param("promotion") GalleryPromotion promotion);

    /**
     * Promotion ID 목록으로 일괄 조회 (N+1 최적화)
     */
    @Query("SELECT gpp FROM GalleryPromotionPayment gpp WHERE gpp.promotion.id IN :promotionIds ORDER BY gpp.createdAt DESC")
    List<GalleryPromotionPayment> findByPromotionIds(@Param("promotionIds") List<Long> promotionIds);
}
