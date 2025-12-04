package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.Payment;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUuid(UUID uuid);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Payment> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);

    /**
     * 특정 엔티티 타입의 결제 내역 조회
     */
    Page<Payment> findByUserAndEntityTypeOrderByCreatedAtDesc(User user, String entityType, Pageable pageable);

    /**
     * 특정 사용자의 크레딧 충전 결제 내역
     */
    @Query("SELECT p FROM Payment p WHERE p.user = :user AND p.entityType = 'CREDIT_PURCHASE' " +
           "AND p.status = 'COMPLETED' ORDER BY p.createdAt DESC")
    Page<Payment> findCreditPurchasesByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 엔티티의 결제 내역
     */
    List<Payment> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * 사용자의 특정 기간 결제 합계
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p " +
           "WHERE p.user = :user AND p.status = 'COMPLETED' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPaymentAmountByUserAndPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * PENDING 상태의 오래된 결제 조회 (정리용)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :expiredBefore")
    List<Payment> findExpiredPendingPayments(@Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * 결제 존재 여부 확인 (transactionId로)
     */
    boolean existsByTransactionId(String transactionId);

    // ========== Admin 쿼리 ==========

    /**
     * 모든 결제 내역 조회 (페이징)
     */
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 상태별 결제 내역 조회 (페이징)
     */
    Page<Payment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 엔티티 타입별 결제 내역 조회 (페이징)
     */
    Page<Payment> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    /**
     * 키워드 검색 (사용자 이메일/이름, transactionId)
     */
    @Query("SELECT p FROM Payment p JOIN p.user u " +
           "WHERE u.email LIKE %:keyword% OR p.transactionId LIKE %:keyword% " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 기간 내 결제 내역
     */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 총 결제 금액 (상태별)
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumPaymentAmountByStatus(@Param("status") String status);

    /**
     * 총 결제 건수
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED'")
    long countCompletedPayments();
}
