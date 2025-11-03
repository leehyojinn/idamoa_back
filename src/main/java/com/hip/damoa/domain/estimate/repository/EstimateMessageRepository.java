package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateMessage;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
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

@Repository
public interface EstimateMessageRepository extends JpaRepository<EstimateMessage, Long> {

    // Find by request
    List<EstimateMessage> findByRequest(EstimateRequest request);

    Page<EstimateMessage> findByRequest(EstimateRequest request, Pageable pageable);

    // Find by proposal
    List<EstimateMessage> findByProposal(EstimateProposal proposal);

    Page<EstimateMessage> findByProposal(EstimateProposal proposal, Pageable pageable);

    // Find by request ordered by time
    @Query("SELECT m FROM EstimateMessage m WHERE m.request = :request " +
           "ORDER BY m.createdAt ASC")
    List<EstimateMessage> findByRequestOrderByCreatedAt(@Param("request") EstimateRequest request);

    // Find by proposal ordered by time
    @Query("SELECT m FROM EstimateMessage m WHERE m.proposal = :proposal " +
           "ORDER BY m.createdAt ASC")
    List<EstimateMessage> findByProposalOrderByCreatedAt(@Param("proposal") EstimateProposal proposal);

    // Find by sender
    List<EstimateMessage> findBySender(User sender);

    Page<EstimateMessage> findBySender(User sender, Pageable pageable);

    // Find unread messages by request
    @Query("SELECT m FROM EstimateMessage m WHERE m.request = :request " +
           "AND m.isRead = false " +
           "ORDER BY m.createdAt DESC")
    List<EstimateMessage> findUnreadByRequest(@Param("request") EstimateRequest request);

    // Find unread messages by proposal
    @Query("SELECT m FROM EstimateMessage m WHERE m.proposal = :proposal " +
           "AND m.isRead = false " +
           "ORDER BY m.createdAt DESC")
    List<EstimateMessage> findUnreadByProposal(@Param("proposal") EstimateProposal proposal);

    // Find messages by time range
    @Query("SELECT m FROM EstimateMessage m WHERE m.createdAt >= :startTime " +
           "AND m.createdAt <= :endTime " +
           "ORDER BY m.createdAt DESC")
    List<EstimateMessage> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    // Count unread by request
    @Query("SELECT COUNT(m) FROM EstimateMessage m WHERE m.request = :request " +
           "AND m.isRead = false")
    long countUnreadByRequest(@Param("request") EstimateRequest request);

    // Count unread by proposal
    @Query("SELECT COUNT(m) FROM EstimateMessage m WHERE m.proposal = :proposal " +
           "AND m.isRead = false")
    long countUnreadByProposal(@Param("proposal") EstimateProposal proposal);

    // Count by request
    long countByRequest(EstimateRequest request);

    // Count by proposal
    long countByProposal(EstimateProposal proposal);

    // Delete by request
    void deleteByRequest(EstimateRequest request);

    // Delete by proposal
    void deleteByProposal(EstimateProposal proposal);
}
