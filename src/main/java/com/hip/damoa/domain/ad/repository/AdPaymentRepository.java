package com.hip.damoa.domain.ad.repository;

import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.ad.model.AdPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdPaymentRepository extends JpaRepository<AdPayment, Long> {

    Optional<AdPayment> findByUuid(UUID uuid);

    List<AdPayment> findByCampaignAndIsDeletedFalse(AdCampaign campaign);

    List<AdPayment> findByCampaignAndStatusAndIsDeletedFalse(AdCampaign campaign, String status);

    /**
     * 캠페인의 활성 결제 중 특정 날짜에 적용되는 결제 조회
     */
    @Query("""
        SELECT ap FROM AdPayment ap
        WHERE ap.campaign = :campaign
          AND ap.status = 'ACTIVE'
          AND ap.isDeleted = false
          AND ap.applyFromDate <= :date
          AND ap.applyToDate >= :date
        ORDER BY ap.createdAt DESC
        """)
    List<AdPayment> findActivePaymentsForDate(
            @Param("campaign") AdCampaign campaign,
            @Param("date") LocalDate date);

    /**
     * 캠페인의 활성 결제 총액 조회
     */
    @Query("""
        SELECT COALESCE(SUM(ap.paymentAmount), 0)
        FROM AdPayment ap
        WHERE ap.campaign = :campaign
          AND ap.status = 'ACTIVE'
          AND ap.isDeleted = false
          AND ap.applyToDate >= :today
        """)
    BigDecimal sumActivePaymentAmount(
            @Param("campaign") AdCampaign campaign,
            @Param("today") LocalDate today);

    /**
     * 캠페인의 1일 가치(dailyValue) 합산
     * 현재 날짜에 적용되는 활성 결제들의 dailyValue 합
     *
     * 예:
     * - 결제1: 7일에 700원 → dailyValue = 100원
     * - 결제2: 6일에 600원 추가 → dailyValue = 100원
     * - 총 dailyValue = 200원
     */
    @Query("""
        SELECT COALESCE(SUM(ap.dailyValue), 0)
        FROM AdPayment ap
        WHERE ap.campaign = :campaign
          AND ap.status = 'ACTIVE'
          AND ap.isDeleted = false
          AND ap.applyFromDate <= :today
          AND ap.applyToDate >= :today
        """)
    BigDecimal sumDailyValueForCampaign(
            @Param("campaign") AdCampaign campaign,
            @Param("today") LocalDate today);

    /**
     * 만료된 결제 조회 (적용 종료일이 지난 ACTIVE 결제)
     */
    @Query("""
        SELECT ap FROM AdPayment ap
        WHERE ap.status = 'ACTIVE'
          AND ap.isDeleted = false
          AND ap.applyToDate < :today
        """)
    List<AdPayment> findExpiredPayments(@Param("today") LocalDate today);

    /**
     * 캠페인별 총 결제 금액 조회
     */
    @Query("""
        SELECT COALESCE(SUM(ap.paymentAmount), 0)
        FROM AdPayment ap
        WHERE ap.campaign = :campaign
          AND ap.status IN ('ACTIVE', 'CONSUMED')
          AND ap.isDeleted = false
        """)
    BigDecimal sumTotalPaymentAmount(@Param("campaign") AdCampaign campaign);

    /**
     * 캠페인 ID로 활성 결제 조회
     */
    @Query("""
        SELECT ap FROM AdPayment ap
        WHERE ap.campaign.id = :campaignId
          AND ap.status = 'ACTIVE'
          AND ap.isDeleted = false
          AND ap.applyToDate >= :today
        ORDER BY ap.applyFromDate ASC
        """)
    List<AdPayment> findActiveByCampaignId(
            @Param("campaignId") Long campaignId,
            @Param("today") LocalDate today);
}
