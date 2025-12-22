package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotionTypeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioPromotionTypeSettingRepository extends JpaRepository<PortfolioPromotionTypeSetting, Long> {

    /**
     * UUID로 조회
     */
    Optional<PortfolioPromotionTypeSetting> findByUuid(UUID uuid);

    /**
     * 프로모션 타입으로 조회
     */
    Optional<PortfolioPromotionTypeSetting> findByPromotionType(String promotionType);

    /**
     * 활성화된 프로모션 타입으로 조회
     */
    Optional<PortfolioPromotionTypeSetting> findByPromotionTypeAndIsActiveTrue(String promotionType);

    /**
     * 모든 설정 조회 (정렬순)
     */
    List<PortfolioPromotionTypeSetting> findAllByOrderByDisplayOrderAsc();

    /**
     * 활성화된 설정만 조회 (정렬순)
     */
    List<PortfolioPromotionTypeSetting> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * 프로모션 타입 존재 여부 확인
     */
    boolean existsByPromotionType(String promotionType);
}
