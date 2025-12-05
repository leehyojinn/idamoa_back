package com.hip.damoa.domain.ad.repository;

import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.company.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    Optional<AdCampaign> findByUuid(UUID uuid);

    Optional<AdCampaign> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 비관적 락과 함께 캠페인 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ac FROM AdCampaign ac WHERE ac.uuid = :uuid AND ac.isDeleted = false")
    Optional<AdCampaign> findWithLockByUuid(@Param("uuid") UUID uuid);

    /**
     * 회사의 활성 캠페인 조회
     */
    Optional<AdCampaign> findByCompanyAndStatusAndIsDeletedFalse(Company company, String status);

    /**
     * 회사의 모든 캠페인 조회 (히스토리)
     */
    List<AdCampaign> findByCompanyAndIsDeletedFalseOrderByCreatedAtDesc(Company company);

    /**
     * 회사의 캠페인 페이징 조회
     */
    Page<AdCampaign> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    /**
     * 활성 캠페인 목록 조회 (우선순위 순)
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        WHERE ac.status = 'ACTIVE'
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        ORDER BY ac.priorityScore DESC, ac.secondaryScore DESC
        """)
    List<AdCampaign> findActiveCampaignsByPriority(@Param("today") LocalDate today);

    /**
     * 활성 캠페인 페이징 조회 (우선순위 순)
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        WHERE ac.status = 'ACTIVE'
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        ORDER BY ac.priorityScore DESC, ac.secondaryScore DESC
        """)
    Page<AdCampaign> findActiveCampaignsByPriority(@Param("today") LocalDate today, Pageable pageable);

    /**
     * 만료된 캠페인 조회 (종료일이 지난 ACTIVE 캠페인)
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        WHERE ac.status = 'ACTIVE'
          AND ac.isDeleted = false
          AND ac.endDate < :today
        """)
    List<AdCampaign> findExpiredCampaigns(@Param("today") LocalDate today);

    /**
     * 우선순위 재계산이 필요한 활성 캠페인 조회
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        WHERE ac.status = 'ACTIVE'
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        """)
    List<AdCampaign> findCampaignsForPriorityUpdate(@Param("today") LocalDate today);

    /**
     * 특정 회사의 활성 LISTING 광고 여부 확인
     */
    @Query("""
        SELECT COUNT(ac) > 0 FROM AdCampaign ac
        WHERE ac.company = :company
          AND ac.status = 'ACTIVE'
          AND ac.adType = com.hip.damoa.domain.ad.model.AdType.LISTING
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        """)
    boolean existsActiveListingCampaign(@Param("company") Company company, @Param("today") LocalDate today);

    /**
     * 활성 LISTING 캠페인이 있는 회사 ID 목록 조회
     */
    @Query("""
        SELECT DISTINCT ac.company.id FROM AdCampaign ac
        WHERE ac.status = 'ACTIVE'
          AND ac.adType = com.hip.damoa.domain.ad.model.AdType.LISTING
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        """)
    List<Long> findCompanyIdsWithActiveListingCampaign(@Param("today") LocalDate today);

    /**
     * 회사별 활성 캠페인과 우선순위 점수 조회
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        WHERE ac.company.id IN :companyIds
          AND ac.status = 'ACTIVE'
          AND ac.adType = com.hip.damoa.domain.ad.model.AdType.LISTING
          AND ac.isDeleted = false
          AND (ac.endDate IS NULL OR ac.endDate >= :today)
        """)
    List<AdCampaign> findActiveListingCampaignsByCompanyIds(
            @Param("companyIds") List<Long> companyIds,
            @Param("today") LocalDate today);

    /**
     * 상태별 캠페인 수 조회
     */
    long countByStatusAndIsDeletedFalse(String status);

    /**
     * 벌크 상태 업데이트
     */
    @Modifying
    @Query("""
        UPDATE AdCampaign ac
        SET ac.status = :newStatus, ac.updatedAt = CURRENT_TIMESTAMP
        WHERE ac.id IN :ids
        """)
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("newStatus") String newStatus);

    // ========== 자동 갱신 관련 쿼리 ==========

    /**
     * 자동 갱신 대상 캠페인 조회
     * - 활성 상태
     * - autoRenew = true
     * - 오늘이 종료일
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        JOIN FETCH ac.company c
        JOIN FETCH c.owner
        WHERE ac.status = 'ACTIVE'
          AND ac.autoRenew = true
          AND ac.isDeleted = false
          AND ac.endDate = :today
        """)
    List<AdCampaign> findCampaignsForAutoRenewal(@Param("today") LocalDate today);

    /**
     * 갱신 알림 대상 캠페인 조회
     * - 활성 상태
     * - autoRenew = true
     * - 아직 알림 미발송 (renewalNotified = false)
     * - 종료일이 targetEndDate 이하 (3일 이내)
     */
    @Query("""
        SELECT ac FROM AdCampaign ac
        JOIN FETCH ac.company c
        JOIN FETCH c.owner
        WHERE ac.status = 'ACTIVE'
          AND ac.autoRenew = true
          AND (ac.renewalNotified = false OR ac.renewalNotified IS NULL)
          AND ac.isDeleted = false
          AND ac.endDate <= :targetEndDate
        """)
    List<AdCampaign> findCampaignsForRenewalNotification(
            @Param("targetEndDate") LocalDate targetEndDate);
}
