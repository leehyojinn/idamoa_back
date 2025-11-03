package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateAttachment;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateAttachmentRepository extends JpaRepository<EstimateAttachment, Long> {

    // Find by request
    List<EstimateAttachment> findByRequest(EstimateRequest request);

    // Find by proposal
    List<EstimateAttachment> findByProposal(EstimateProposal proposal);

    // Find by file name
    Optional<EstimateAttachment> findByFileName(String fileName);

    // Find by file type
    List<EstimateAttachment> findByFileType(String fileType);

    // Find by request and file type
    List<EstimateAttachment> findByRequestAndFileType(EstimateRequest request, String fileType);

    // Find by proposal and file type
    List<EstimateAttachment> findByProposalAndFileType(EstimateProposal proposal, String fileType);

    // Calculate total file size by request
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM EstimateAttachment a " +
           "WHERE a.request = :request")
    Long calculateTotalFileSizeByRequest(@Param("request") EstimateRequest request);

    // Calculate total file size by proposal
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM EstimateAttachment a " +
           "WHERE a.proposal = :proposal")
    Long calculateTotalFileSizeByProposal(@Param("proposal") EstimateProposal proposal);

    // Find most downloaded attachments
    @Query("SELECT a FROM EstimateAttachment a " +
           "ORDER BY a.downloadCount DESC")
    List<EstimateAttachment> findMostDownloaded(@Param("limit") int limit);

    // Count by request
    long countByRequest(EstimateRequest request);

    // Count by proposal
    long countByProposal(EstimateProposal proposal);

    // Delete by request
    void deleteByRequest(EstimateRequest request);

    // Delete by proposal
    void deleteByProposal(EstimateProposal proposal);
}
