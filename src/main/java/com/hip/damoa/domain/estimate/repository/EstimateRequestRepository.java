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
import java.util.UUID;

@Repository
public interface EstimateRequestRepository extends JpaRepository<EstimateRequest, Long> {

    // Find by ID (not deleted)
    Optional<EstimateRequest> findByIdAndIsDeletedFalse(Long id);

    // Find by UUID (not deleted)
    Optional<EstimateRequest> findByUuidAndIsDeletedFalse(UUID uuid);

    // Find by user
    Page<EstimateRequest> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    List<EstimateRequest> findByUserAndIsDeletedFalse(User user);

    // Find by status
    Page<EstimateRequest> findByStatusAndIsDeletedFalse(EstimateRequest.EstimateStatus status, Pageable pageable);

    // Find by status and isPublic
    @Query("SELECT e FROM EstimateRequest e WHERE e.status = :status AND e.isPublic = :isPublic AND e.isDeleted = false")
    Page<EstimateRequest> findByStatusAndVisibilityAndIsDeletedFalse(@Param("status") EstimateRequest.EstimateStatus status, @Param("isPublic") boolean isPublic, Pageable pageable);

    // Find public requests (for companies to browse)
    @Query("SELECT e FROM EstimateRequest e WHERE e.isPublic = true AND e.isDeleted = false " +
           "AND e.status = :status ORDER BY e.createdAt DESC")
    Page<EstimateRequest> findPublicRequests(@Param("status") EstimateRequest.EstimateStatus status, Pageable pageable);

    // Find active public requests (published and not expired)
    @Query("SELECT e FROM EstimateRequest e WHERE e.isPublic = true AND e.isDeleted = false " +
           "AND e.status = com.hip.damoa.domain.estimate.model.EstimateRequest$EstimateStatus.PUBLISHED " +
           "AND (e.expiresAt IS NULL OR e.expiresAt > :now) " +
           "ORDER BY e.createdAt DESC")
    Page<EstimateRequest> findActivePublicRequests(@Param("now") LocalDateTime now, Pageable pageable);

    // Count by user
    long countByUserAndIsDeletedFalse(User user);

    // Count by status
    @Query("SELECT COUNT(e) FROM EstimateRequest e WHERE e.status = :status AND e.isDeleted = false")
    long countByStatusAndIsDeletedFalse(@Param("status") EstimateRequest.EstimateStatus status);

    // Find expired requests
    @Query("SELECT e FROM EstimateRequest e WHERE e.isDeleted = false " +
           "AND e.status = com.hip.damoa.domain.estimate.model.EstimateRequest$EstimateStatus.PUBLISHED " +
           "AND e.expiresAt IS NOT NULL " +
           "AND e.expiresAt < :now")
    List<EstimateRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    // Find by status (for admin)
    Page<EstimateRequest> findByStatus(EstimateRequest.EstimateStatus status, Pageable pageable);
}
