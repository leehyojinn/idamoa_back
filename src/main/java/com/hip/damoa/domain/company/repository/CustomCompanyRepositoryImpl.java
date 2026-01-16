package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 동적 정렬을 지원하는 Company Repository 구현
 */
@Slf4j
@Repository
public class CustomCompanyRepositoryImpl implements CustomCompanyRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Company> searchWithDynamicSort(
            String keyword,
            BigDecimal minRating,
            boolean hasFilters,
            List<Long> filterOptionIds,
            Integer categoryCount,
            String sortBy,
            Pageable pageable) {

        // ORDER BY 절 생성
        String orderByClause = buildOrderByClause(sortBy);

        // 동적 WHERE 절 생성
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE c.is_deleted = false AND c.status = 'ACTIVE'");

        if (minRating != null) {
            whereClause.append(" AND c.avg_rating >= :minRating");
        }

        if (hasFilters && filterOptionIds != null && !filterOptionIds.isEmpty()) {
            // JSONB 배열에서 필터 옵션을 검색 (V77 마이그레이션 이후)
            // 각 카테고리에서 최소 1개의 옵션이 매칭되어야 함
            whereClause.append("""
                 AND (
                    SELECT COUNT(DISTINCT fo.category_id)
                    FROM filter_options fo
                    WHERE fo.id IN (:filterOptionIds)
                    AND jsonb_contains_id(c.filter_option_ids, fo.id)
                ) = :categoryCount
                """);
        }

        if (keyword != null && !keyword.isEmpty()) {
            whereClause.append("""
                 AND (
                    LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.detail_content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR EXISTS (
                        SELECT 1 FROM unnest(c.tags) AS tag
                        WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                )
                """);
        }

        // 메인 쿼리 - 서브쿼리로 광고 정보를 가져와서 DISTINCT 문제 해결
        String sql = """
            SELECT c.* FROM companies c
            LEFT JOIN LATERAL (
                SELECT ac.id as ad_id, ac.priority_score, ac.secondary_score
                FROM ad_campaigns ac
                WHERE ac.company_id = c.id
                AND ac.status = 'ACTIVE'
                AND ac.ad_type = 'LISTING'
                AND (ac.end_date IS NULL OR ac.end_date >= CURRENT_DATE)
                AND ac.is_deleted = false
                ORDER BY ac.priority_score DESC
                LIMIT 1
            ) ac ON true
            """ + whereClause + orderByClause;

        // 카운트 쿼리
        String countSql = "SELECT COUNT(c.id) FROM companies c " + whereClause;

        // 메인 쿼리 실행
        Query query = entityManager.createNativeQuery(sql, Company.class);
        setQueryParameters(query, keyword, minRating, hasFilters, filterOptionIds, categoryCount);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Company> results = query.getResultList();

        // 카운트 쿼리 실행
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, keyword, minRating, hasFilters, filterOptionIds, categoryCount);

        long total = ((Number) countQuery.getSingleResult()).longValue();

        log.debug("업체 검색 완료: sortBy={}, results={}, total={}", sortBy, results.size(), total);

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 쿼리 파라미터 설정 (NULL이 아닌 값만)
     */
    private void setQueryParameters(Query query, String keyword, BigDecimal minRating,
                                     boolean hasFilters, List<Long> filterOptionIds, Integer categoryCount) {
        if (minRating != null) {
            query.setParameter("minRating", minRating);
        }
        if (hasFilters && filterOptionIds != null && !filterOptionIds.isEmpty()) {
            query.setParameter("filterOptionIds", filterOptionIds);
            query.setParameter("categoryCount", categoryCount != null ? categoryCount : 0);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.setParameter("keyword", keyword);
        }
    }

    /**
     * sortBy에 따른 ORDER BY 절 생성
     *
     * 기본 정렬 순서: 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 → 평점
     *
     * sortBy 옵션:
     * - AD_PRIORITY: 광고 우선순위 (기본값)
     * - LATEST: 최신순
     * - LIKE_COUNT: 좋아요순
     * - REVIEW_COUNT: 리뷰순
     * - VIEW_COUNT: 조회수순
     * - RATING: 평점순
     * - PREMIUM_TIER: 프리미엄순
     */
    private String buildOrderByClause(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "AD_PRIORITY";
        }

        // 기본 정렬: owner_id 유무 → 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 → 평점
        String defaultSort = """
            CASE WHEN c.owner_id IS NOT NULL THEN 0 ELSE 1 END,
            CASE WHEN ac.ad_id IS NOT NULL THEN 0 ELSE 1 END,
            COALESCE(ac.priority_score, 0) DESC,
            c.like_count DESC,
            c.review_count DESC,
            c.view_count DESC,
            c.avg_rating DESC NULLS LAST
            """;

        String primarySort;
        switch (sortBy.toUpperCase()) {
            case "LATEST":
                primarySort = "c.created_at DESC";
                break;
            case "LIKE_COUNT":
            case "POPULAR":  // 하위 호환
                primarySort = "c.like_count DESC";
                break;
            case "VIEW_COUNT":
                primarySort = "c.view_count DESC";
                break;
            case "RATING":
                primarySort = "c.avg_rating DESC NULLS LAST";
                break;
            case "REVIEW_COUNT":
                primarySort = "c.review_count DESC";
                break;
            case "PREMIUM_TIER":
                primarySort = "c.premium_monthly_amount DESC NULLS LAST, c.premium_tier DESC NULLS LAST";
                break;
            case "AD_PRIORITY":
            default:
                // 광고 우선순위가 1순위인 경우 (기본값)
                return " ORDER BY " + defaultSort;
        }

        // sortBy가 지정된 경우: 1순위 sortBy, 2순위 이후 기본 정렬
        return " ORDER BY " + primarySort + ", " + defaultSort;
    }
}
