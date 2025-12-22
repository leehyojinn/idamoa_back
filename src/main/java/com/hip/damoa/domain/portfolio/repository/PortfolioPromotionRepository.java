package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotion;
import com.hip.damoa.domain.portfolio.model.PortfolioPromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioPromotionRepository extends JpaRepository<PortfolioPromotion, Long> {

    /**
     * UUID로 조회
     */
    Optional<PortfolioPromotion> findByUuid(UUID uuid);

    /**
     * 포트폴리오의 활성 프로모션 조회 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio p " +
           "WHERE pp.portfolio.id = :portfolioId " +
           "AND pp.status = :status " +
           "AND pp.isDeleted = false " +
           "AND p.isDeleted = false")
    Optional<PortfolioPromotion> findByPortfolioIdAndStatusAndIsDeletedFalse(
            @Param("portfolioId") Long portfolioId,
            @Param("status") PortfolioPromotionStatus status);

    /**
     * 포트폴리오의 활성 프로모션 조회 (간편 버전)
     */
    default Optional<PortfolioPromotion> findActiveByPortfolioId(Long portfolioId) {
        return findByPortfolioIdAndStatusAndIsDeletedFalse(portfolioId, PortfolioPromotionStatus.ACTIVE);
    }

    /**
     * 사용자의 모든 활성 프로모션 조회 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio p " +
           "WHERE pp.user.id = :userId " +
           "AND pp.status = :status " +
           "AND pp.isDeleted = false " +
           "AND p.isDeleted = false")
    List<PortfolioPromotion> findByUserIdAndStatusAndIsDeletedFalse(
            @Param("userId") Long userId,
            @Param("status") PortfolioPromotionStatus status);

    /**
     * 사용자의 모든 활성 프로모션 조회 (간편 버전)
     */
    default List<PortfolioPromotion> findActiveByUserId(Long userId) {
        return findByUserIdAndStatusAndIsDeletedFalse(userId, PortfolioPromotionStatus.ACTIVE);
    }

    /**
     * 모든 활성 프로모션 조회 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio p " +
           "WHERE pp.status = :status " +
           "AND pp.isDeleted = false " +
           "AND p.isDeleted = false")
    List<PortfolioPromotion> findByStatusAndIsDeletedFalse(
            @Param("status") PortfolioPromotionStatus status);

    /**
     * 모든 활성 프로모션 조회 (간편 버전)
     */
    default List<PortfolioPromotion> findAllActive() {
        return findByStatusAndIsDeletedFalse(PortfolioPromotionStatus.ACTIVE);
    }

    /**
     * 모든 프로모션 조회 (상태 무관, 포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio p " +
           "WHERE pp.isDeleted = false " +
           "AND p.isDeleted = false " +
           "ORDER BY pp.createdAt DESC")
    List<PortfolioPromotion> findAllByIsDeletedFalse();

    /**
     * 만료 예정 프로모션 조회 (지정 날짜 이전, 포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio p " +
           "WHERE pp.status = :status " +
           "AND pp.endDate <= :targetDate " +
           "AND pp.isDeleted = false " +
           "AND p.isDeleted = false")
    List<PortfolioPromotion> findExpiringSoon(
            @Param("status") PortfolioPromotionStatus status,
            @Param("targetDate") LocalDate targetDate);

    /**
     * 만료된 프로모션 조회 (오늘 날짜 기준, 포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio pf " +
           "WHERE pp.status = 'ACTIVE' " +
           "AND pp.endDate < :today " +
           "AND pp.isDeleted = false " +
           "AND pf.isDeleted = false")
    List<PortfolioPromotion> findExpiredPromotions(@Param("today") LocalDate today);

    /**
     * 자동갱신 대상 프로모션 조회 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio pf " +
           "WHERE pp.status = 'ACTIVE' " +
           "AND pp.autoRenew = true " +
           "AND pp.endDate = :targetDate " +
           "AND pp.isDeleted = false " +
           "AND pf.isDeleted = false")
    List<PortfolioPromotion> findAutoRenewTargets(@Param("targetDate") LocalDate targetDate);

    /**
     * 갱신 알림 미발송 프로모션 조회 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT pp FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio pf " +
           "WHERE pp.status = 'ACTIVE' " +
           "AND pp.renewalNotified = false " +
           "AND pp.endDate <= :targetDate " +
           "AND pp.isDeleted = false " +
           "AND pf.isDeleted = false")
    List<PortfolioPromotion> findUnnotifiedExpiringSoon(@Param("targetDate") LocalDate targetDate);

    /**
     * 포트폴리오에 활성 프로모션이 있는지 확인 (포트폴리오 삭제 여부 포함)
     */
    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END " +
           "FROM PortfolioPromotion pp " +
           "JOIN pp.portfolio pf " +
           "WHERE pp.portfolio.id = :portfolioId " +
           "AND pp.status = :status " +
           "AND pp.isDeleted = false " +
           "AND pf.isDeleted = false")
    boolean existsByPortfolioIdAndStatusAndIsDeletedFalse(
            @Param("portfolioId") Long portfolioId,
            @Param("status") PortfolioPromotionStatus status);

    /**
     * 포트폴리오에 활성 프로모션이 있는지 확인 (간편 버전)
     */
    default boolean hasActivePromotion(Long portfolioId) {
        return existsByPortfolioIdAndStatusAndIsDeletedFalse(portfolioId, PortfolioPromotionStatus.ACTIVE);
    }
}
