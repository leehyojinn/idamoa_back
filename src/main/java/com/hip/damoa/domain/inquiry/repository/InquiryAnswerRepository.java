package com.hip.damoa.domain.inquiry.repository;

import com.hip.damoa.domain.inquiry.model.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 일반 문의 답변 리포지토리
 */
@Repository
public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {

    /**
     * 문의 ID로 답변 조회
     */
    Optional<InquiryAnswer> findByInquiryId(Long inquiryId);

    /**
     * 문의 UUID로 답변 조회
     */
    @Query("SELECT ia FROM InquiryAnswer ia " +
           "JOIN ia.inquiry i " +
           "WHERE i.uuid = :inquiryUuid " +
           "AND i.isDeleted = false " +
           "AND ia.isDeleted = false")
    Optional<InquiryAnswer> findByInquiryUuid(@Param("inquiryUuid") UUID inquiryUuid);

    /**
     * 문의에 답변이 존재하는지 확인
     */
    boolean existsByInquiryId(Long inquiryId);

    /**
     * 문의 ID로 답변 삭제
     */
    void deleteByInquiryId(Long inquiryId);
}