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
     * 통합 키워드 검색
     * 검색 대상: 업체명, 설명, 상세내용, 주소, 웹사이트URL, 이메일
     * tags는 별도 hasTags 메서드 사용, serviceAreas는 필터로 검색, keywords는 제거됨
     */
    public static Specification<Company> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";

            // 일반 텍스트 필드 검색만 수행
            // tags 배열은 PostgreSQL 호환성 문제로 별도 검색
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("detailContent")), pattern),
                cb.like(cb.lower(root.get("address")), pattern),
                cb.like(cb.lower(root.get("websiteUrl")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("primaryPhone")), pattern)
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
     * 서비스 지역 필터 - 필터 옵션을 통한 검색
     * REGION 카테고리의 필터 옵션 ID들로 검색
     */
    public static Specification<Company> hasRegionFilters(List<Long> regionFilterIds) {
        return (root, query, cb) -> {
            if (regionFilterIds == null || regionFilterIds.isEmpty()) {
                return cb.conjunction();
            }

            // EXISTS 서브쿼리로 필터 옵션 확인
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CompanyFilterOption> cfoRoot = subquery.from(CompanyFilterOption.class);

            subquery.select(cfoRoot.get("company").get("id"))
                .where(
                    cb.equal(cfoRoot.get("company").get("id"), root.get("id")),
                    cfoRoot.get("filterOption").get("id").in(regionFilterIds)
                );

            return cb.exists(subquery);
        };
    }

    /**
     * @deprecated 서비스 지역은 이제 필터 옵션으로 관리됨. hasRegionFilters 사용 권장
     * 임시로 마이그레이션 기간 동안 유지
     */
    @Deprecated
    public static Specification<Company> hasServiceAreas(String[] serviceAreas) {
        return (root, query, cb) -> {
            if (serviceAreas == null || serviceAreas.length == 0) {
                return cb.conjunction();
            }

            // PostgreSQL array_position 함수를 사용하여 배열 멤버십 체크
            // array_position(service_areas, 'value') IS NOT NULL
            Predicate[] predicates = new Predicate[serviceAreas.length];
            for (int i = 0; i < serviceAreas.length; i++) {
                predicates[i] = cb.isNotNull(
                    cb.function("array_position", Integer.class,
                        root.get("serviceAreas"),
                        cb.literal(serviceAreas[i])
                    )
                );
            }
            return cb.or(predicates);
        };
    }

    /**
     * 태그 필터 (PostgreSQL 배열 연산)
     * 각 tag 값이 배열에 포함되어 있는지 확인 (OR 조건)
     * tags는 text[] 배열로 유지됨 (사용자 자유 입력용)
     */
    public static Specification<Company> hasTags(String[] tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.length == 0) {
                return cb.conjunction();
            }

            // PostgreSQL array_position 함수를 사용하여 배열 멤버십 체크
            // array_position(tags, 'value') IS NOT NULL
            Predicate[] predicates = new Predicate[tags.length];
            for (int i = 0; i < tags.length; i++) {
                predicates[i] = cb.isNotNull(
                    cb.function("array_position", Integer.class,
                        root.get("tags"),
                        cb.literal(tags[i])
                    )
                );
            }
            return cb.or(predicates);
        };
    }
}
