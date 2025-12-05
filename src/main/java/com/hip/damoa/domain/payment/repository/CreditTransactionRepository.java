package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.CreditTransaction;
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
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    Optional<CreditTransaction> findByUuid(UUID uuid);

    Page<CreditTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user.id = :userId ORDER BY ct.createdAt DESC")
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    List<CreditTransaction> findByUserAndTransactionType(User user, String transactionType);

    /**
     * 특정 기간 내 거래 내역
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user = :user " +
           "AND ct.createdAt BETWEEN :startDate AND :endDate ORDER BY ct.createdAt DESC")
    List<CreditTransaction> findByUserAndPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 엔티티에 대한 트랜잭션 조회
     */
    List<CreditTransaction> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * 특정 사용자의 특정 엔티티 관련 트랜잭션
     */
    @Query("SELECT ct FROM CreditTransaction ct WHERE ct.user = :user " +
           "AND ct.entityType = :entityType AND ct.entityId = :entityId")
    List<CreditTransaction> findByUserAndEntity(
            @Param("user") User user,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId
    );

    /**
     * 특정 타입의 총 금액 조회
     */
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CreditTransaction ct " +
           "WHERE ct.user = :user AND ct.transactionType = :type")
    BigDecimal sumAmountByUserAndType(
            @Param("user") User user,
            @Param("type") String type
    );

    /**
     * 최근 N개 거래 내역 조회
     */
    List<CreditTransaction> findTop10ByUserOrderByCreatedAtDesc(User user);

    // ========== Admin 쿼리 ==========

    /**
     * 모든 거래 내역 조회 (페이징)
     */
    Page<CreditTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 거래 유형별 조회
     */
    Page<CreditTransaction> findByTransactionTypeOrderByCreatedAtDesc(String transactionType, Pageable pageable);

    /**
     * 키워드 검색 (사용자 이메일/이름, 사유)
     */
    @Query("SELECT ct FROM CreditTransaction ct JOIN ct.user u " +
           "WHERE u.email LIKE %:keyword% OR ct.reason LIKE %:keyword% " +
           "ORDER BY ct.createdAt DESC")
    Page<CreditTransaction> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 기간 내 모든 거래 내역
     */
    @Query("SELECT ct FROM CreditTransaction ct " +
           "WHERE ct.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ct.createdAt DESC")
    Page<CreditTransaction> findByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
