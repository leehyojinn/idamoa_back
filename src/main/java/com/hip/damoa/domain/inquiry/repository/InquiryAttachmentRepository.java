package com.hip.damoa.domain.inquiry.repository;

import com.hip.damoa.domain.inquiry.model.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 일반 문의 첨부파일 리포지토리
 */
@Repository
public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {

    /**
     * 문의 ID로 첨부파일 목록 조회 (삭제되지 않은 것만, 표시 순서대로)
     */
    List<InquiryAttachment> findByInquiryIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long inquiryId);

    /**
     * 문의 ID와 파일 ID로 첨부파일 조회
     */
    Optional<InquiryAttachment> findByInquiryIdAndFileIdAndIsDeletedFalse(Long inquiryId, Long fileId);

    /**
     * 문의 ID로 첨부파일 존재 여부 확인
     */
    boolean existsByInquiryIdAndIsDeletedFalse(Long inquiryId);

    /**
     * 문의 ID로 첨부파일 개수 조회
     */
    long countByInquiryIdAndIsDeletedFalse(Long inquiryId);

    /**
     * 문의 ID로 모든 첨부파일 삭제 (Soft Delete)
     */
    void deleteByInquiryId(Long inquiryId);
}