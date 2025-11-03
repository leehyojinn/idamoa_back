package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
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
public interface EstimateRequestRepository extends JpaRepository<EstimateRequest, Long> {

    // Find by ID (not deleted)
    Optional<EstimateRequest> findByIdAndIsDeletedFalse(Long id);

    // Find by user
    Page<EstimateRequest> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    List<EstimateRequest> findByUserAndIsDeletedFalse(User user);

    // Find by status
    Page<EstimateRequest> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    // Find by status and visibility
    Page<EstimateRequest> findByStatusAndVisibilityAndIsDeletedFalse(String status, String visibility, Pageable pageable);

    // Find public requests (for companies to browse)
    @Query("SELECT e FROM EstimateRequest e WHERE e.visibility = 'PUBLIC' AND e.isDeleted = false " +
           "AND e.status = :status ORDER BY e.createdAt DESC")
    Page<EstimateRequest> findPublicRequests(@Param("status") String status, Pageable pageable);

    // Find active public requests (submitted and not expired)
    @Query("SELECT e FROM EstimateRequest e WHERE e.visibility = 'PUBLIC' AND e.isDeleted = false " +
           "AND e.status = 'SUBMITTED' " +
           "AND (e.deadline IS NULL OR e.deadline > :now) " +
           "ORDER BY e.createdAt DESC")
    Page<EstimateRequest> findActivePublicRequests(@Param("now") LocalDateTime now, Pageable pageable);

    // Count by user
    long countByUserAndIsDeletedFalse(User user);

    // Count by status
    long countByStatusAndIsDeletedFalse(String status);

    // Find expired requests
    @Query("SELECT e FROM EstimateRequest e WHERE e.isDeleted = false " +
           "AND e.status = 'SUBMITTED' " +
           "AND e.deadline IS NOT NULL " +
           "AND e.deadline < :now")
    List<EstimateRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    // Find by status (for admin)
    Page<EstimateRequest> findByStatus(String status, Pageable pageable);
}
