package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.model.Match;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    // Find by ID (not deleted)
    Optional<Match> findByIdAndIsDeletedFalse(Long id);

    // Find by user (client)
    Page<Match> findByRequest_UserAndIsDeletedFalse(User user, Pageable pageable);

    List<Match> findByRequest_UserAndIsDeletedFalse(User user);

    // Find by company
    Page<Match> findByProposal_CompanyAndIsDeletedFalse(Company company, Pageable pageable);

    List<Match> findByProposal_CompanyAndIsDeletedFalse(Company company);

    // Find by estimate request
    Optional<Match> findByRequestAndIsDeletedFalse(EstimateRequest request);

    // Find by estimate proposal
    Optional<Match> findByProposalAndIsDeletedFalse(EstimateProposal proposal);

    // Find by status
    Page<Match> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    // Find by user and status
    @Query("SELECT m FROM Match m WHERE m.request.user = :user AND m.status = :status " +
           "AND m.isDeleted = false ORDER BY m.confirmedAt DESC")
    Page<Match> findByUserAndStatus(
        @Param("user") User user,
        @Param("status") String status,
        Pageable pageable
    );

    // Find by company and status
    @Query("SELECT m FROM Match m WHERE m.proposal.company = :company AND m.status = :status " +
           "AND m.isDeleted = false ORDER BY m.confirmedAt DESC")
    Page<Match> findByCompanyAndStatus(
        @Param("company") Company company,
        @Param("status") String status,
        Pageable pageable
    );

    // Find in-progress matches
    @Query("SELECT m FROM Match m WHERE m.status = 'IN_PROGRESS' " +
           "AND m.isDeleted = false ORDER BY m.startedAt DESC")
    Page<Match> findInProgressMatches(Pageable pageable);

    // Find completed matches for company (for calculating stats)
    @Query("SELECT m FROM Match m WHERE m.proposal.company = :company " +
           "AND m.status = 'COMPLETED' AND m.isDeleted = false")
    List<Match> findCompletedMatchesByCompany(@Param("company") Company company);

    // Count matches by user
    long countByRequest_UserAndIsDeletedFalse(User user);

    // Count matches by company
    long countByProposal_CompanyAndIsDeletedFalse(Company company);

    // Count completed matches by company
    long countByProposal_CompanyAndStatusAndIsDeletedFalse(Company company, String status);

    // Check if estimate request already has a match
    boolean existsByRequestAndIsDeletedFalse(EstimateRequest request);
}
