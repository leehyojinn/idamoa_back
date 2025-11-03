package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.Coupon;
import com.hip.damoa.domain.payment.model.UserCoupon;
import com.hip.damoa.domain.user.model.User;
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
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // Find by user
    List<UserCoupon> findByUser(User user);

    Page<UserCoupon> findByUser(User user, Pageable pageable);

    // Find by user and coupon
    Optional<UserCoupon> findByUserAndCoupon(User user, Coupon coupon);

    // Find by coupon
    List<UserCoupon> findByCoupon(Coupon coupon);

    Page<UserCoupon> findByCoupon(Coupon coupon, Pageable pageable);

    // Find available coupons for user
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user = :user " +
           "AND uc.isUsed = false " +
           "AND uc.coupon.validUntil > :now " +
           "ORDER BY uc.coupon.validUntil ASC")
    List<UserCoupon> findAvailableByUser(@Param("user") User user,
                                          @Param("now") LocalDateTime now);

    // Find used coupons by user
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user = :user " +
           "AND uc.isUsed = true " +
           "ORDER BY uc.usedAt DESC")
    List<UserCoupon> findUsedByUser(@Param("user") User user);

    Page<UserCoupon> findUsedByUser(@Param("user") User user, Pageable pageable);

    // Find expired coupons
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.isUsed = false " +
           "AND uc.coupon.validUntil < :now")
    List<UserCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    // Find expiring soon coupons
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user = :user " +
           "AND uc.isUsed = false " +
           "AND uc.coupon.validUntil > :now " +
           "AND uc.coupon.validUntil <= :threshold " +
           "ORDER BY uc.coupon.validUntil ASC")
    List<UserCoupon> findExpiringSoonByUser(@Param("user") User user,
                                             @Param("now") LocalDateTime now,
                                             @Param("threshold") LocalDateTime threshold);

    // Count available coupons for user
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user = :user " +
           "AND uc.isUsed = false " +
           "AND uc.coupon.validUntil > :now")
    long countAvailableByUser(@Param("user") User user,
                               @Param("now") LocalDateTime now);

    // Count used coupons for user
    long countByUserAndIsUsedTrue(User user);

    // Count by coupon
    long countByCoupon(Coupon coupon);

    // Count used by coupon
    long countByCouponAndIsUsedTrue(Coupon coupon);

    // Check if user has specific coupon
    boolean existsByUserAndCoupon(User user, Coupon coupon);

    // Check if user has unused coupon
    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END " +
           "FROM UserCoupon uc WHERE uc.user = :user " +
           "AND uc.coupon = :coupon " +
           "AND uc.isUsed = false " +
           "AND uc.coupon.validUntil > :now")
    boolean hasUnusedCoupon(@Param("user") User user,
                            @Param("coupon") Coupon coupon,
                            @Param("now") LocalDateTime now);
}
