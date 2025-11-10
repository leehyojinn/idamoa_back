package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyFilterOption;
import com.hip.damoa.domain.filter.model.FilterCategory;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Company 검색을 위한 JPA Specifications
 *
 * 필터 로직:
 * - 같은 카테고리 내: OR 조건 (내과 OR 외과)
 * - 다른 카테고리 간: AND 조건 ((내과 OR 외과) AND (서울 OR 경기))
 */
public class CompanySpecifications {

    /**
     * 삭제되지 않은 업체만 조회
     */
    public static Specification<Company> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    /**
     * 활성 상태 업체만 조회
     */
    public static Specification<Company> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
    }

    /**
     * 키워드 검색 (업체명 또는 설명)
     */
    public static Specification<Company> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /**
     * 최소 평점 필터
     */
    public static Specification<Company> hasMinRating(BigDecimal minRating) {
        return (root, query, cb) -> {
            if (minRating == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("avgRating"), minRating);
        };
    }

    /**
     * 카테고리별 필터 옵션 (OR within category, AND across categories)
     *
     * @param filterOptionsByCategory Map<CategoryId, List<OptionIds>>
     *                                 예: {1: [1,2], 2: [10,11]} = (옵션1 OR 옵션2) AND (옵션10 OR 옵션11)
     */
    public static Specification<Company> hasFilterOptions(Map<Long, List<Long>> filterOptionsByCategory) {
        return (root, query, cb) -> {
            if (filterOptionsByCategory == null || filterOptionsByCategory.isEmpty()) {
                return cb.conjunction();
            }

            // DISTINCT 설정 (중복 제거)
            query.distinct(true);

            // 각 카테고리별로 AND 조건 생성
            Predicate[] categoryPredicates = filterOptionsByCategory.entrySet().stream()
                .map(entry -> {
                    List<Long> optionIds = entry.getValue();

                    // 각 카테고리에 대해 EXISTS 서브쿼리 생성
                    // (회사가 해당 카테고리의 옵션 중 하나라도 가지고 있는지)
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<CompanyFilterOption> cfoRoot = subquery.from(CompanyFilterOption.class);

                    subquery.select(cfoRoot.get("company").get("id"))
                        .where(
                            cb.equal(cfoRoot.get("company").get("id"), root.get("id")),
                            cfoRoot.get("filterOption").get("id").in(optionIds)
                        );

                    return cb.exists(subquery);
                })
                .toArray(Predicate[]::new);

            // 모든 카테고리 조건을 AND로 결합
            return cb.and(categoryPredicates);
        };
    }

    /**
     * 서비스 지역 필터 (PostgreSQL 배열 연산)
     */
    public static Specification<Company> hasServiceAreas(String[] serviceAreas) {
        return (root, query, cb) -> {
            if (serviceAreas == null || serviceAreas.length == 0) {
                return cb.conjunction();
            }
            // PostgreSQL 배열 && 연산자 사용 (겹치는 요소가 있는지 확인)
            return cb.isTrue(
                cb.function("array_overlap", Boolean.class,
                    root.get("serviceAreas"),
                    cb.literal(serviceAreas)
                )
            );
        };
    }

    /**
     * 태그 필터 (PostgreSQL 배열 연산)
     */
    public static Specification<Company> hasTags(String[] tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.length == 0) {
                return cb.conjunction();
            }
            // PostgreSQL 배열 && 연산자 사용
            return cb.isTrue(
                cb.function("array_overlap", Boolean.class,
                    root.get("tags"),
                    cb.literal(tags)
                )
            );
        };
    }
}
