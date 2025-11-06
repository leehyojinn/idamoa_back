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

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByIdAndIsDeletedFalse(Long id);

    Optional<Company> findByOwnerAndIsDeletedFalse(User owner);

    Optional<Company> findByOwnerId(Long ownerId);

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Company> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    /**
     * 업체 검색 (PostgreSQL 배열 필터 지원)
     *
     * @param keyword 검색 키워드 (업체명, 설명)
     * @param serviceAreas 서비스 지역 배열 (OR 조건)
     * @param tags 태그 배열 (OR 조건)
     * @param minRating 최소 평점
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query(value = """
        SELECT * FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (CAST(:serviceAreasSize AS INTEGER) = 0 OR c.service_areas && CAST(:serviceAreas AS text[]))
        AND (CAST(:tagsSize AS INTEGER) = 0 OR c.tags && CAST(:tags AS text[]))
        """,
        countQuery = """
        SELECT COUNT(*) FROM companies c
        WHERE c.is_deleted = false
        AND c.status = 'ACTIVE'
        AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minRating IS NULL OR c.avg_rating >= :minRating)
        AND (CAST(:serviceAreasSize AS INTEGER) = 0 OR c.service_areas && CAST(:serviceAreas AS text[]))
        AND (CAST(:tagsSize AS INTEGER) = 0 OR c.tags && CAST(:tags AS text[]))
        """,
        nativeQuery = true)
    Page<Company> searchCompaniesWithFilters(
        @Param("keyword") String keyword,
        @Param("serviceAreas") String[] serviceAreas,
        @Param("serviceAreasSize") Integer serviceAreasSize,
        @Param("tags") String[] tags,
        @Param("tagsSize") Integer tagsSize,
        @Param("minRating") BigDecimal minRating,
        Pageable pageable
    );

    // Check if user already has a company
    boolean existsByOwnerEmailAndIsDeletedFalse(String ownerEmail);
}
