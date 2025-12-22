package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.CompanyPartnership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyPartnershipRepository extends JpaRepository<CompanyPartnership, Long> {

    /**
     * UUID로 조회
     */
    Optional<CompanyPartnership> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 전체 목록 조회 (삭제되지 않은)
     */
    Page<CompanyPartnership> findByIsDeletedFalseOrderByDisplayOrderAsc(Pageable pageable);

    /**
     * 상태별 목록 조회
     */
    @Query("SELECT p FROM CompanyPartnership p " +
           "WHERE p.status = :status " +
           "AND p.isDeleted = false " +
           "ORDER BY p.displayOrder ASC")
    Page<CompanyPartnership> findByStatusAndIsDeletedFalse(
        @Param("status") String status, Pageable pageable);

    /**
     * 해당 업체에 활성 제휴가 있는지 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM CompanyPartnership p " +
           "WHERE p.company.id = :companyId " +
           "AND p.status = 'ACTIVE' " +
           "AND p.isDeleted = false")
    boolean existsActiveByCompanyId(@Param("companyId") Long companyId);

    /**
     * 활성 제휴업체 목록 조회 (displayOrder 순)
     */
    @Query("SELECT p FROM CompanyPartnership p " +
           "JOIN FETCH p.company c " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.isDeleted = false " +
           "AND c.isDeleted = false " +
           "AND p.startDate <= :today " +
           "AND p.endDate >= :today " +
           "ORDER BY p.displayOrder ASC, p.createdAt ASC")
    List<CompanyPartnership> findActivePartnerships(@Param("today") LocalDate today);

    /**
     * 특정 업체의 활성 제휴 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM CompanyPartnership p " +
           "WHERE p.company.uuid = :companyUuid " +
           "AND p.status = 'ACTIVE' " +
           "AND p.isDeleted = false " +
           "AND p.startDate <= :today " +
           "AND p.endDate >= :today")
    boolean existsActivePartnershipByCompanyUuid(
        @Param("companyUuid") UUID companyUuid,
        @Param("today") LocalDate today);

    /**
     * 만료된 제휴 조회 (스케줄러용)
     */
    @Query("SELECT p FROM CompanyPartnership p " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.isDeleted = false " +
           "AND p.endDate < :today")
    List<CompanyPartnership> findExpiredPartnerships(@Param("today") LocalDate today);

    /**
     * 특정 업체의 제휴 이력 조회
     */
    @Query("SELECT p FROM CompanyPartnership p " +
           "WHERE p.company.uuid = :companyUuid " +
           "AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    List<CompanyPartnership> findByCompanyUuid(@Param("companyUuid") UUID companyUuid);

    /**
     * 전체 목록 (관리자용, 상태 무관)
     */
    @Query("SELECT p FROM CompanyPartnership p " +
           "JOIN FETCH p.company c " +
           "WHERE p.isDeleted = false " +
           "ORDER BY p.displayOrder ASC, p.createdAt DESC")
    Page<CompanyPartnership> findAllForAdmin(Pageable pageable);
}
