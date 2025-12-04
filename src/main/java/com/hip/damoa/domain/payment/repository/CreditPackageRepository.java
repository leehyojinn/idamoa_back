package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.CreditPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditPackageRepository extends JpaRepository<CreditPackage, Long> {

    // UUID로 조회
    Optional<CreditPackage> findByUuid(UUID uuid);

    Optional<CreditPackage> findByUuidAndIsDeletedFalse(UUID uuid);

    // 코드로 조회
    Optional<CreditPackage> findByCode(String code);

    Optional<CreditPackage> findByCodeAndIsDeletedFalse(String code);

    // 단위 금액으로 조회
    Optional<CreditPackage> findByUnitAmountAndIsDeletedFalse(Integer unitAmount);

    // 활성 패키지만 조회 (4개)
    List<CreditPackage> findByIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc();

    // 중복 확인
    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByUnitAmountAndIsDeletedFalse(Integer unitAmount);

    // 관리자용 전체 조회
    Page<CreditPackage> findByIsDeletedFalse(Pageable pageable);

    // 관리자용 검색
    @Query("""
        SELECT cp FROM CreditPackage cp
        WHERE cp.isDeleted = false
        AND (:unitAmount IS NULL OR cp.unitAmount = :unitAmount)
        AND (:isActive IS NULL OR cp.isActive = :isActive)
        ORDER BY cp.displayOrder ASC
        """)
    Page<CreditPackage> searchForAdmin(
            @Param("unitAmount") Integer unitAmount,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // 통계
    long countByIsActiveTrueAndIsDeletedFalse();
}
