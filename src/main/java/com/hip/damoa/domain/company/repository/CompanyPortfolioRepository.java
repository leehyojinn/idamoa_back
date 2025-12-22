package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyPortfolioRepository extends JpaRepository<CompanyPortfolio, Long>, JpaSpecificationExecutor<CompanyPortfolio> {

    /**
     * UUID로 조회
     */
    Optional<CompanyPortfolio> findByUuid(UUID uuid);

    /**
     * UUID로 조회 (삭제되지 않은 것만)
     */
    Optional<CompanyPortfolio> findByUuidAndIsDeletedFalse(UUID uuid);

    /**
     * 업체의 포트폴리오 목록 (페이징)
     */
    Page<CompanyPortfolio> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    /**
     * 업체 UUID로 포트폴리오 목록 조회
     */
    @Query("SELECT p FROM CompanyPortfolio p WHERE p.company.uuid = :companyUuid AND p.isDeleted = false")
    Page<CompanyPortfolio> findByCompanyUuidAndIsDeletedFalse(@Param("companyUuid") UUID companyUuid, Pageable pageable);

    /**
     * 업체의 공개 포트폴리오 목록 (페이징)
     */
    @Query("SELECT p FROM CompanyPortfolio p WHERE p.company.uuid = :companyUuid AND p.isPublic = true AND p.isDeleted = false")
    Page<CompanyPortfolio> findPublicByCompanyUuid(@Param("companyUuid") UUID companyUuid, Pageable pageable);

    /**
     * 사용자가 작성한 포트폴리오 목록
     */
    @Query("SELECT p FROM CompanyPortfolio p WHERE p.company.owner.id = :userId AND p.isDeleted = false")
    Page<CompanyPortfolio> findByOwnerId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 대표 포트폴리오 조회
     */
    List<CompanyPortfolio> findByCompanyAndIsFeaturedAndIsDeletedFalse(Company company, Boolean isFeatured);

    /**
     * 공개된 포트폴리오 전체 조회 (페이징)
     */
    Page<CompanyPortfolio> findByIsPublicTrueAndIsDeletedFalse(Pageable pageable);

    /**
     * ID 목록으로 조회
     */
    List<CompanyPortfolio> findByIdIn(List<Long> ids);

    /**
     * 업체별 포트폴리오 수
     */
    long countByCompanyAndIsDeletedFalse(Company company);

    /**
     * 관리자용 전체 포트폴리오 검색 (키워드 검색만)
     */
    @Query("SELECT p FROM CompanyPortfolio p " +
           "WHERE p.isDeleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(COALESCE(CAST(p.content AS string), '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(COALESCE(p.company.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CompanyPortfolio> searchForAdmin(
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 활성 프로모션이 있는 포트폴리오 조회 (사용자용 추천 목록)
     */
    @Query("SELECT DISTINCT p FROM CompanyPortfolio p " +
           "JOIN PortfolioPromotion pp ON pp.portfolio = p " +
           "WHERE p.isDeleted = false " +
           "AND pp.status = 'ACTIVE' " +
           "AND pp.isDeleted = false")
    Page<CompanyPortfolio> findWithActivePromotion(Pageable pageable);
}
