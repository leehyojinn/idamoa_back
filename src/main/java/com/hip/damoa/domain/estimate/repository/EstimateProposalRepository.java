package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.user.model.User;
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
public interface EstimateProposalRepository extends JpaRepository<EstimateProposal, Long> {

    // Find by ID (not deleted)
    Optional<EstimateProposal> findByIdAndIsDeletedFalse(Long id);

    // Find by UUID (not deleted)
    Optional<EstimateProposal> findByUuidAndIsDeletedFalse(UUID uuid);

    // Find by UUID (관리자용 - 삭제된 데이터 포함)
    Optional<EstimateProposal> findByUuid(UUID uuid);

    // Find by estimate request
    List<EstimateProposal> findByRequestAndIsDeletedFalse(EstimateRequest request);

    Page<EstimateProposal> findByRequestAndIsDeletedFalse(EstimateRequest request, Pageable pageable);

    // Find by company
    Page<EstimateProposal> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    List<EstimateProposal> findByCompanyAndIsDeletedFalse(Company company);

    // Find by estimate request and company (check if already proposed)
    Optional<EstimateProposal> findByRequestAndCompanyAndIsDeletedFalse(
        EstimateRequest request,
        Company company
    );

    // Check if company already proposed to this request
    boolean existsByRequestAndCompanyAndIsDeletedFalse(
        EstimateRequest request,
        Company company
    );

    // Find by status
    Page<EstimateProposal> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    // Find by company and status
    @Query("SELECT p FROM EstimateProposal p WHERE p.company = :company " +
           "AND p.status = :status AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<EstimateProposal> findByCompanyAndStatus(
        @Param("company") Company company,
        @Param("status") String status,
        Pageable pageable
    );

    // Count proposals by estimate request
    long countByRequestAndIsDeletedFalse(EstimateRequest request);

    // Count proposals by company
    long countByCompanyAndIsDeletedFalse(Company company);

    // Find unviewed proposals by client
    @Query("SELECT p FROM EstimateProposal p WHERE p.request.user = :user " +
           "AND p.status = 'SUBMITTED' AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<EstimateProposal> findUnviewedProposalsByClient(@Param("user") User user);

    // Count unviewed proposals
    long countByRequest_UserAndStatusAndIsDeletedFalse(User user, String status);

    // Additional methods for simplified access
    boolean existsByRequestAndCompany(EstimateRequest request, Company company);

    Page<EstimateProposal> findByCompany(Company company, Pageable pageable);

    List<EstimateProposal> findByRequest(EstimateRequest request);

    // WITHDRAWN 상태를 제외한 제안 체크 및 조회
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM EstimateProposal p " +
           "WHERE p.request = :request AND p.company = :company " +
           "AND p.isDeleted = false AND p.status != 'WITHDRAWN'")
    boolean existsByRequestAndCompanyAndStatusNotWithdrawn(
        @Param("request") EstimateRequest request,
        @Param("company") Company company
    );

    @Query("SELECT p FROM EstimateProposal p WHERE p.request = :request " +
           "AND p.isDeleted = false AND p.status != 'WITHDRAWN' " +
           "ORDER BY p.createdAt DESC")
    List<EstimateProposal> findByRequestAndStatusNotWithdrawn(@Param("request") EstimateRequest request);

    // Admin Dashboard Statistics
    Long countByIsDeletedFalse();

    Long countByCreatedAtAfterAndIsDeletedFalse(java.time.LocalDateTime dateTime);

    Long countByStatusAndIsDeletedFalse(String status);

    Long countByIsSelectedTrueAndIsDeletedFalse();

    @Query("SELECT AVG(p.price) FROM EstimateProposal p WHERE p.isDeleted = false")
    java.math.BigDecimal getAveragePrice();
}
