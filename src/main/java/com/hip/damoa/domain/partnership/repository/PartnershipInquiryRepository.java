package com.hip.damoa.domain.partnership.repository;

import com.hip.damoa.domain.partnership.model.PartnershipInquiry;
import com.hip.damoa.domain.partnership.model.PartnershipStatus;
import com.hip.damoa.domain.partnership.model.PartnershipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 제휴/광고 문의 리포지토리
 */
@Repository
public interface PartnershipInquiryRepository extends JpaRepository<PartnershipInquiry, Long> {

    // UUID로 조회
    Optional<PartnershipInquiry> findByUuidAndIsDeletedFalse(UUID uuid);

    // 전체 목록 조회 (관리자용)
    Page<PartnershipInquiry> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 상태별 조회
    Page<PartnershipInquiry> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            PartnershipStatus status, Pageable pageable);

    // 유형별 조회
    Page<PartnershipInquiry> findByPartnershipTypeAndIsDeletedFalseOrderByCreatedAtDesc(
            PartnershipType type, Pageable pageable);

    // 상태와 유형별 조회
    Page<PartnershipInquiry> findByStatusAndPartnershipTypeAndIsDeletedFalseOrderByCreatedAtDesc(
            PartnershipStatus status, PartnershipType type, Pageable pageable);

    // 상태별 개수
    long countByStatusAndIsDeletedFalse(PartnershipStatus status);
}