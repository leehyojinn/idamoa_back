package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByIdAndIsDeletedFalse(Long id);

    Optional<Company> findByUuid(UUID uuid);

    Optional<Company> findByUuidAndIsDeletedFalse(UUID uuid);

    Optional<Company> findByOwnerAndIsDeletedFalse(User owner);

    Optional<Company> findByOwnerId(Long ownerId);

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Company> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    /**
     * 업체 검색 (PostgreSQL 배열 필터 + 필터 옵션 지원)
     *
     * @param keyword 검색 키워드 (업체명, 설명)
     * @param serviceAreas 서비스 지역 배열 (OR 조건)
     * @param tags 태그 배열 (OR 조건)
     * @param minRating 최소 평점
     * @param filterOptionIds 필터 옵션 ID 배열 (AND 조건 - 모든 필터를 만족해야 함)
     * @param filterCount 필터 옵션 개수
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query(value = """
        SELECT DISTINCT c.* FROM companies c
        LEFT JOIN company_filter_options cfo ON c.id = cfo.company_id
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (CAST(:serviceAreasSize AS INTEGER) = 0 OR c.service_areas && CAST(:serviceAreas AS text[]))
        AND (CAST(:tagsSize AS INTEGER) = 0 OR c.tags && CAST(:tags AS text[]))
        AND (CAST(:filterCount AS INTEGER) = 0 OR cfo.filter_option_id IN (:filterOptionIds))
        ORDER BY c.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id) FROM companies c
        LEFT JOIN company_filter_options cfo ON c.id = cfo.company_id
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (CAST(:serviceAreasSize AS INTEGER) = 0 OR c.service_areas && CAST(:serviceAreas AS text[]))
        AND (CAST(:tagsSize AS INTEGER) = 0 OR c.tags && CAST(:tags AS text[]))
        AND (CAST(:filterCount AS INTEGER) = 0 OR cfo.filter_option_id IN (:filterOptionIds))
        """,
        nativeQuery = true)
    Page<Company> searchCompaniesWithFilters(
        @Param("keyword") String keyword,
        @Param("serviceAreas") String[] serviceAreas,
        @Param("serviceAreasSize") Integer serviceAreasSize,
        @Param("tags") String[] tags,
        @Param("tagsSize") Integer tagsSize,
        @Param("minRating") BigDecimal minRating,
        @Param("filterOptionIds") Long[] filterOptionIds,
        @Param("filterCount") Integer filterCount,
        Pageable pageable
    );

    /**
     * 통합 키워드 검색 (태그 포함)
     * Native Query로 태그 배열 내부의 텍스트까지 검색
     */
    @Query(value = """
        SELECT DISTINCT c.* FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.detail_content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.website_url) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.primary_phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR EXISTS (
                SELECT 1 FROM unnest(c.tags) AS tag
                WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        )
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id) FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.detail_content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.website_url) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.primary_phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR EXISTS (
                SELECT 1 FROM unnest(c.tags) AS tag
                WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        )
        """,
        nativeQuery = true)
    Page<Company> searchByKeywordIncludingTags(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // Check if user already has a company
    boolean existsByOwnerEmailAndIsDeletedFalse(String ownerEmail);

    // Admin Dashboard Statistics
    Long countByIsDeletedFalse();

    Long countByStatusAndIsDeletedFalse(String status);

    Long countByCreatedAtAfterAndIsDeletedFalse(java.time.LocalDateTime dateTime);

    Long countByVerifiedTrueAndIsDeletedFalse();

    Long countByPremiumUntilAfterAndIsDeletedFalse(java.time.LocalDateTime dateTime);

    Long countByPremiumTierAndIsDeletedFalse(String premiumTier);

    @Query("SELECT AVG(c.avgRating) FROM Company c WHERE c.isDeleted = false")
    BigDecimal getAverageRating();

    @Query("SELECT SUM(c.reviewCount) FROM Company c WHERE c.isDeleted = false")
    Long getTotalReviewCount();

    /**
     * 필터와 키워드를 모두 적용한 통합 검색
     * 필터가 1순위로 적용되고, 키워드는 필터링된 결과 내에서 검색
     */
    @Query(value = """
        SELECT DISTINCT c.* FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (:hasFilters = false OR (
            SELECT COUNT(DISTINCT fo.category_id)
            FROM company_filter_options cfo
            JOIN filter_options fo ON cfo.filter_option_id = fo.id
            WHERE cfo.company_id = c.id
            AND cfo.filter_option_id IN (:filterOptionIds)
        ) = :categoryCount)
        AND (:keyword IS NULL OR :keyword = '' OR (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.detail_content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.website_url) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.primary_phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR EXISTS (
                SELECT 1 FROM unnest(c.tags) AS tag
                WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        ))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id) FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (:hasFilters = false OR (
            SELECT COUNT(DISTINCT fo.category_id)
            FROM company_filter_options cfo
            JOIN filter_options fo ON cfo.filter_option_id = fo.id
            WHERE cfo.company_id = c.id
            AND cfo.filter_option_id IN (:filterOptionIds)
        ) = :categoryCount)
        AND (:keyword IS NULL OR :keyword = '' OR (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.detail_content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.website_url) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.primary_phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR EXISTS (
                SELECT 1 FROM unnest(c.tags) AS tag
                WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        ))
        """,
        nativeQuery = true)
    Page<Company> searchWithFiltersAndKeyword(
        @Param("keyword") String keyword,
        @Param("minRating") BigDecimal minRating,
        @Param("hasFilters") boolean hasFilters,
        @Param("filterOptionIds") Long[] filterOptionIds,
        @Param("categoryCount") Integer categoryCount,
        Pageable pageable
    );
}
