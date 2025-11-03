package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // Find by code
    Optional<Coupon> findByCode(String code);

    // Find by status
    List<Coupon> findByStatus(String status);

    Page<Coupon> findByStatus(String status, Pageable pageable);

    // Find active coupons
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' " +
           "AND c.validFrom <= :now AND c.validUntil >= :now " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' " +
           "AND c.validFrom <= :now AND c.validUntil >= :now " +
           "ORDER BY c.createdAt DESC")
    Page<Coupon> findActiveCoupons(@Param("now") LocalDateTime now, Pageable pageable);

    // Find expired coupons
    @Query("SELECT c FROM Coupon c WHERE c.validUntil < :now " +
           "AND c.status != 'EXPIRED'")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    // Find by discount type
    List<Coupon> findByDiscountType(String discountType);

    Page<Coupon> findByDiscountType(String discountType, Pageable pageable);

    // Find available coupons (not reached max usage)
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' " +
           "AND c.validFrom <= :now AND c.validUntil >= :now " +
           "AND (c.maxUsageCount IS NULL OR c.currentUsageCount < c.maxUsageCount)")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    // Find coupons by valid date range
    @Query("SELECT c FROM Coupon c WHERE c.validFrom >= :startDate " +
           "AND c.validUntil <= :endDate")
    List<Coupon> findByValidDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Find most used coupons
    @Query("SELECT c FROM Coupon c ORDER BY c.currentUsageCount DESC")
    List<Coupon> findMostUsedCoupons(Pageable pageable);

    // Count by status
    long countByStatus(String status);

    // Count active coupons
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = 'ACTIVE' " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    long countActiveCoupons(@Param("now") LocalDateTime now);

    // Check if code exists
    boolean existsByCode(String code);
}
