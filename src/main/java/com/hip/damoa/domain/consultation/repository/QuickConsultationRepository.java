package com.hip.damoa.domain.consultation.repository;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuickConsultationRepository extends JpaRepository<QuickConsultation, Long> {

    // UUID 기반 조회
    Optional<QuickConsultation> findByUuidAndIsDeletedFalse(UUID uuid);

    // 사용자별 조회
    Page<QuickConsultation> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    // 상태별 조회 (관리자용)
    Page<QuickConsultation> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ConsultationStatus status, Pageable pageable);

    // 전체 조회 (관리자용)
    Page<QuickConsultation> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 사용자 + 상태별 조회
    Page<QuickConsultation> findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            User user, ConsultationStatus status, Pageable pageable);

    // 개수 조회
    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(ConsultationStatus status);

    long countByUserAndIsDeletedFalse(User user);
}
