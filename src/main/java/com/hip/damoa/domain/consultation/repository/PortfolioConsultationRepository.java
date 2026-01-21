package com.hip.damoa.domain.consultation.repository;

import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioConsultationRepository extends JpaRepository<PortfolioConsultation, Long> {

    Optional<PortfolioConsultation> findByUuidAndIsDeletedFalse(UUID uuid);

    // 사용자별 상담신청 목록
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.user.id = :userId AND pc.isDeleted = false " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 업체별 상담신청 목록
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.company.uuid = :companyUuid AND pc.isDeleted = false " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByCompanyUuid(@Param("companyUuid") UUID companyUuid, Pageable pageable);

    // 업체별 + 상태별 상담신청 목록
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.company.uuid = :companyUuid AND pc.status = :status AND pc.isDeleted = false " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByCompanyUuidAndStatus(
            @Param("companyUuid") UUID companyUuid,
            @Param("status") PortfolioConsultationStatus status,
            Pageable pageable);

    // 포트폴리오별 상담신청 수
    @Query("SELECT COUNT(pc) FROM PortfolioConsultation pc " +
           "WHERE pc.portfolio.uuid = :portfolioUuid AND pc.isDeleted = false")
    long countByPortfolioUuid(@Param("portfolioUuid") UUID portfolioUuid);

    // 업체별 미처리 상담신청 수
    @Query("SELECT COUNT(pc) FROM PortfolioConsultation pc " +
           "WHERE pc.company.uuid = :companyUuid " +
           "AND pc.status = 'PENDING' AND pc.isDeleted = false")
    long countPendingByCompanyUuid(@Param("companyUuid") UUID companyUuid);

    // ===== Admin용 쿼리 =====

    // UUID로 조회 (삭제 여부 무관)
    Optional<PortfolioConsultation> findByUuid(UUID uuid);

    // 전체 목록 (삭제 여부 무관)
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findAllForAdmin(Pageable pageable);

    // 삭제 여부로 필터링
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.isDeleted = :isDeleted " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByIsDeleted(@Param("isDeleted") Boolean isDeleted, Pageable pageable);

    // 상태로 필터링 (삭제 여부 무관)
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.status = :status " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByStatusForAdmin(@Param("status") PortfolioConsultationStatus status, Pageable pageable);

    // 상태 + 삭제 여부로 필터링
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE pc.status = :status AND pc.isDeleted = :isDeleted " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> findByStatusAndIsDeleted(
            @Param("status") PortfolioConsultationStatus status,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);

    // 키워드 검색 (이름, 이메일, 제목, 내용)
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE (LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 키워드 + 삭제 여부로 검색
    @Query("SELECT pc FROM PortfolioConsultation pc " +
           "WHERE (LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pc.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND pc.isDeleted = :isDeleted " +
           "ORDER BY pc.createdAt DESC")
    Page<PortfolioConsultation> searchByKeywordAndIsDeleted(
            @Param("keyword") String keyword,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);
}
