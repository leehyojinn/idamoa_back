package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.EstimateRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EstimateRequestAttachmentRepository extends JpaRepository<EstimateRequestAttachment, Long> {

    /**
     * 견적 요청의 모든 첨부파일 조회 (순서대로)
     */
    List<EstimateRequestAttachment> findByEstimateRequestIdOrderByDisplayOrderAsc(Long estimateRequestId);

    /**
     * 견적 요청의 특정 파일 ID로 첨부파일 조회
     */
    @Query("SELECT a FROM EstimateRequestAttachment a WHERE a.estimateRequest.id = :requestId AND a.fileId = :fileId")
    EstimateRequestAttachment findByEstimateRequestIdAndFileId(
        @Param("requestId") Long requestId,
        @Param("fileId") Long fileId
    );

    /**
     * 견적 요청의 모든 첨부파일 삭제
     */
    void deleteByEstimateRequestId(Long estimateRequestId);

    /**
     * 특정 파일이 사용되고 있는지 확인
     */
    boolean existsByFileId(Long fileId);
}
