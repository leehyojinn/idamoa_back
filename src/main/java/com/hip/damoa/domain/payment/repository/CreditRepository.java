package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.Credit;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {

    Optional<Credit> findByUser(User user);

    @Query("SELECT c FROM Credit c WHERE c.user.id = :userId")
    Optional<Credit> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Credit c JOIN c.user u WHERE u.email = :email")
    Optional<Credit> findByUserEmail(@Param("email") String email);

    Optional<Credit> findByUuid(UUID uuid);

    boolean existsByUser(User user);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Credit c WHERE c.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    /**
     * 비관적 락으로 크레딧 조회 (동시성 제어)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.user = :user")
    Optional<Credit> findWithLockByUser(@Param("user") User user);

    /**
     * 비관적 락으로 크레딧 조회 (userId로)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.user.id = :userId")
    Optional<Credit> findWithLockByUserId(@Param("userId") Long userId);

    // ========== Admin 쿼리 ==========

    /**
     * 모든 크레딧 목록 조회 (페이징)
     */
    @Query("SELECT c FROM Credit c JOIN FETCH c.user ORDER BY c.availableCredits DESC")
    Page<Credit> findAllWithUser(Pageable pageable);

    /**
     * 이메일로 검색
     */
    @Query("SELECT c FROM Credit c JOIN c.user u WHERE u.email LIKE %:keyword%")
    Page<Credit> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 총 가용 크레딧 합계
     */
    @Query("SELECT COALESCE(SUM(c.availableCredits), 0) FROM Credit c")
    BigDecimal sumTotalAvailableCredits();

    /**
     * 총 적립 크레딧 합계
     */
    @Query("SELECT COALESCE(SUM(c.totalEarned), 0) FROM Credit c")
    BigDecimal sumTotalEarnedCredits();

    /**
     * 총 사용 크레딧 합계
     */
    @Query("SELECT COALESCE(SUM(c.totalSpent), 0) FROM Credit c")
    BigDecimal sumTotalSpentCredits();

    /**
     * 크레딧 보유 사용자 수
     */
    @Query("SELECT COUNT(c) FROM Credit c WHERE c.availableCredits > 0")
    long countUsersWithCredits();
}
