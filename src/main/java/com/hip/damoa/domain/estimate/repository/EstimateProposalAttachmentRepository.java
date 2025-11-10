package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateProposalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EstimateProposalAttachmentRepository extends JpaRepository<EstimateProposalAttachment, Long> {

    /**
     * 견적 제안의 모든 첨부파일 조회 (순서대로)
     */
    List<EstimateProposalAttachment> findByEstimateProposalIdOrderByDisplayOrderAsc(Long estimateProposalId);

    /**
     * 견적 제안의 특정 파일 ID로 첨부파일 조회
     */
    @Query("SELECT a FROM EstimateProposalAttachment a WHERE a.estimateProposal.id = :proposalId AND a.fileId = :fileId")
    EstimateProposalAttachment findByEstimateProposalIdAndFileId(
        @Param("proposalId") Long proposalId,
        @Param("fileId") Long fileId
    );

    /**
     * 견적 제안의 모든 첨부파일 삭제
     */
    void deleteByEstimateProposalId(Long estimateProposalId);

    /**
     * 특정 파일이 사용되고 있는지 확인
     */
    boolean existsByFileId(Long fileId);
}
