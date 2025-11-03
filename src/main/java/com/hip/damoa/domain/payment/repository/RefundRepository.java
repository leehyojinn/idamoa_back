package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.Payment;
import com.hip.damoa.domain.payment.model.Refund;
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

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // Find by payment
    List<Refund> findByPayment(Payment payment);

    Optional<Refund> findByPaymentAndStatus(Payment payment, String status);

    // Find by status
    List<Refund> findByStatus(String status);

    Page<Refund> findByStatus(String status, Pageable pageable);

    // Find pending refunds
    @Query("SELECT r FROM Refund r WHERE r.status = 'PENDING' " +
           "ORDER BY r.createdAt ASC")
    List<Refund> findPendingRefunds();

    @Query("SELECT r FROM Refund r WHERE r.status = 'PENDING' " +
           "ORDER BY r.createdAt ASC")
    Page<Refund> findPendingRefunds(Pageable pageable);

    // Find completed refunds
    @Query("SELECT r FROM Refund r WHERE r.status = 'COMPLETED' " +
           "ORDER BY r.processedAt DESC")
    Page<Refund> findCompletedRefunds(Pageable pageable);

    // Find by date range
    @Query("SELECT r FROM Refund r WHERE r.createdAt >= :startDate " +
           "AND r.createdAt <= :endDate " +
           "ORDER BY r.createdAt DESC")
    List<Refund> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Find by processed date range
    @Query("SELECT r FROM Refund r WHERE r.processedAt >= :startDate " +
           "AND r.processedAt <= :endDate " +
           "ORDER BY r.processedAt DESC")
    List<Refund> findByProcessedDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Calculate total refund amount by status
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r " +
           "WHERE r.status = :status")
    BigDecimal calculateTotalRefundByStatus(@Param("status") String status);

    // Calculate total refund amount for payment
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r " +
           "WHERE r.payment = :payment AND r.status = 'COMPLETED'")
    BigDecimal calculateTotalRefundByPayment(@Param("payment") Payment payment);

    // Count by status
    long countByStatus(String status);

    // Count by payment
    long countByPayment(Payment payment);

    // Check if refund exists for payment
    boolean existsByPaymentAndStatus(Payment payment, String status);
}
