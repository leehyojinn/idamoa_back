package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.GalleryPromotionTypeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalleryPromotionTypeSettingRepository extends JpaRepository<GalleryPromotionTypeSetting, Long> {

    /**
     * UUID로 설정 조회
     */
    Optional<GalleryPromotionTypeSetting> findByUuid(UUID uuid);

    /**
     * 프로모션 타입으로 설정 조회
     */
    Optional<GalleryPromotionTypeSetting> findByPromotionType(String promotionType);

    /**
     * 활성화된 프로모션 타입으로 설정 조회
     */
    Optional<GalleryPromotionTypeSetting> findByPromotionTypeAndIsActiveTrue(String promotionType);

    /**
     * 모든 설정 조회 (정렬순)
     */
    List<GalleryPromotionTypeSetting> findAllByOrderByDisplayOrderAsc();

    /**
     * 활성화된 설정만 조회 (정렬순)
     */
    List<GalleryPromotionTypeSetting> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * 프로모션 타입 존재 여부 확인
     */
    boolean existsByPromotionType(String promotionType);

    /**
     * 활성화된 프로모션 타입 개수
     */
    @Query("SELECT COUNT(g) FROM GalleryPromotionTypeSetting g WHERE g.isActive = true")
    long countActiveTypes();
}
