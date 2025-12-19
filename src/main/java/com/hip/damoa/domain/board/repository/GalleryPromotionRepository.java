package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.GalleryPromotion;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryPromotionRepository extends JpaRepository<GalleryPromotion, Long> {

    // ===== 기본 조회 =====

    Optional<GalleryPromotion> findByUuid(UUID uuid);

    Optional<GalleryPromotion> findByUuidAndIsDeletedFalse(UUID uuid);

    // ===== Board 기준 조회 =====

    Optional<GalleryPromotion> findByBoardAndStatusAndIsDeletedFalse(Board board, String status);

    Optional<GalleryPromotion> findByBoardIdAndStatusAndIsDeletedFalse(Long boardId, String status);

    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.board.uuid = :boardUuid AND gp.status = 'ACTIVE' AND gp.isDeleted = false")
    Optional<GalleryPromotion> findActiveByBoardUuid(@Param("boardUuid") UUID boardUuid);

    // ===== User 기준 조회 =====

    List<GalleryPromotion> findByUserAndStatusAndIsDeletedFalse(User user, String status);

    Page<GalleryPromotion> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.user.id = :userId AND gp.status = 'ACTIVE' AND gp.isDeleted = false")
    List<GalleryPromotion> findActiveByUserId(@Param("userId") Long userId);

    // ===== 상태 기준 조회 =====

    List<GalleryPromotion> findByStatusAndIsDeletedFalse(String status);

    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.status = 'ACTIVE' AND gp.isDeleted = false")
    List<GalleryPromotion> findAllActive();

    // ===== 스케줄러용 조회 =====

    /**
     * 만료 대상 조회 (종료일 < 오늘, 활성 상태)
     */
    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.status = 'ACTIVE' AND gp.endDate < :today AND gp.isDeleted = false")
    List<GalleryPromotion> findExpiredPromotions(@Param("today") LocalDate today);

    /**
     * 자동 갱신 대상 조회 (자동갱신 ON, 종료일 = 오늘, 활성 상태)
     */
    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.autoRenew = true AND gp.status = 'ACTIVE' AND gp.endDate = :today AND gp.isDeleted = false")
    List<GalleryPromotion> findAutoRenewalTargets(@Param("today") LocalDate today);

    /**
     * 갱신 알림 대상 조회 (자동갱신 ON, 알림 미발송, 종료일 3일 전 이내, 활성 상태)
     */
    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.autoRenew = true AND gp.renewalNotified = false AND gp.status = 'ACTIVE' AND gp.endDate <= :notificationDate AND gp.isDeleted = false")
    List<GalleryPromotion> findRenewalNotificationTargets(@Param("notificationDate") LocalDate notificationDate);

    // ===== 필터별 우대 갤러리 조회 =====

    /**
     * 특정 필터 옵션을 가진 활성 우대 갤러리 조회
     */
    @Query("SELECT DISTINCT gp FROM GalleryPromotion gp " +
           "JOIN BoardFilterOption bfo ON bfo.board = gp.board " +
           "WHERE gp.status = 'ACTIVE' AND gp.isDeleted = false " +
           "AND bfo.filterOption.id IN :filterOptionIds " +
           "AND gp.board.isDeleted = false AND gp.board.isPublished = true")
    List<GalleryPromotion> findActiveByFilterOptionIds(@Param("filterOptionIds") List<Long> filterOptionIds);

    // ===== 통계/카운트 =====

    long countByUserAndStatusAndIsDeletedFalse(User user, String status);

    long countByStatusAndIsDeletedFalse(String status);

    // ===== Board ID 목록으로 일괄 조회 (N+1 최적화) =====

    @Query("SELECT gp FROM GalleryPromotion gp WHERE gp.board.id IN :boardIds AND gp.status = 'ACTIVE' AND gp.isDeleted = false")
    List<GalleryPromotion> findActiveByBoardIds(@Param("boardIds") List<Long> boardIds);
}
