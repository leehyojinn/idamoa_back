package com.hip.damoa.domain.popup.repository;

import com.hip.damoa.domain.popup.model.Popup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {

    /**
     * UUID로 팝업 조회 (삭제되지 않은 것만)
     */
    Optional<Popup> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * UUID로 팝업 조회 (삭제 여부 무관)
     */
    Optional<Popup> findByUuid(UUID uuid);

    /**
     * 활성 팝업 목록 조회 (노출 순서대로)
     */
    List<Popup> findByIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc();

    /**
     * 현재 시간 기준 노출 가능한 활성 팝업 목록 조회
     *
     * 조건:
     * - isActive = true
     * - isDeleted = false
     * - displayStartDate가 null이거나 현재 시간 이전
     * - displayEndDate가 null이거나 현재 시간 이후
     */
    @Query("SELECT p FROM Popup p WHERE p.isActive = true AND p.isDeleted = false " +
           "AND (p.displayStartDate IS NULL OR p.displayStartDate <= :now) " +
           "AND (p.displayEndDate IS NULL OR p.displayEndDate >= :now) " +
           "ORDER BY p.displayOrder ASC")
    List<Popup> findDisplayablePopups(@Param("now") LocalDateTime now);

    /**
     * 전체 팝업 목록 조회 (관리자용, 페이징)
     */
    Page<Popup> findByIsDeletedFalse(Pageable pageable);

    /**
     * 활성 팝업 목록 조회 (관리자용, 페이징)
     */
    Page<Popup> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    /**
     * 팝업 존재 여부 확인
     */
    boolean existsByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 전체 팝업 수 (삭제되지 않은 것만)
     */
    long countByIsDeletedFalse();

    /**
     * 활성 팝업 수
     */
    long countByIsActiveTrueAndIsDeletedFalse();
}
