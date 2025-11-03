package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateItem;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EstimateItemRepository extends JpaRepository<EstimateItem, Long> {

    // Find by proposal
    List<EstimateItem> findByProposal(EstimateProposal proposal);

    // Find by proposal ordered by display order
    @Query("SELECT i FROM EstimateItem i WHERE i.proposal = :proposal " +
           "ORDER BY i.displayOrder ASC")
    List<EstimateItem> findByProposalOrderByDisplayOrder(@Param("proposal") EstimateProposal proposal);

    // Find by category
    List<EstimateItem> findByCategory(String category);

    // Find by proposal and category
    List<EstimateItem> findByProposalAndCategory(EstimateProposal proposal, String category);

    // Calculate total for proposal
    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM EstimateItem i " +
           "WHERE i.proposal = :proposal")
    BigDecimal calculateTotalByProposal(@Param("proposal") EstimateProposal proposal);

    // Calculate total by category
    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM EstimateItem i " +
           "WHERE i.proposal = :proposal AND i.category = :category")
    BigDecimal calculateTotalByCategory(@Param("proposal") EstimateProposal proposal,
                                         @Param("category") String category);

    // Count by proposal
    long countByProposal(EstimateProposal proposal);

    // Count by category
    long countByProposalAndCategory(EstimateProposal proposal, String category);

    // Delete by proposal
    void deleteByProposal(EstimateProposal proposal);
}
