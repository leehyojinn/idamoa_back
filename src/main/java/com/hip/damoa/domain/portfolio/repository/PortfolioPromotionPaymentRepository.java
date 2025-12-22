package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioPromotionPaymentRepository extends JpaRepository<PortfolioPromotionPayment, Long> {

    /**
     * UUID로 조회
     */
    Optional<PortfolioPromotionPayment> findByUuid(UUID uuid);

    /**
     * 프로모션의 모든 결제 내역 조회 (최신순)
     */
    List<PortfolioPromotionPayment> findByPromotionIdOrderByPaymentDateDesc(Long promotionId);

    /**
     * 프로모션의 완료된 결제 내역 조회
     */
    List<PortfolioPromotionPayment> findByPromotionIdAndStatusOrderByPaymentDateDesc(Long promotionId, String status);

    /**
     * 크레딧 트랜잭션 ID로 조회
     */
    Optional<PortfolioPromotionPayment> findByCreditTransactionId(Long creditTransactionId);
}
