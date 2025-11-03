package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.web.dto.CompanySearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Company 동적 검색을 위한 Specification
 */
public class CompanySpecification {

    /**
     * 검색 조건에 따른 Specification 생성
     */
    public static Specification<Company> search(CompanySearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 삭제되지 않은 업체만
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            // 2. 활성 상태 업체만
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));

            // 3. 키워드 검색 (업체명 또는 설명)
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), keyword);
                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), keyword);
                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }

            // 4. 서비스 지역 필터
            if (request.getServiceAreas() != null && request.getServiceAreas().length > 0) {
                List<Predicate> areaPredicates = new ArrayList<>();
                for (String area : request.getServiceAreas()) {
                    // PostgreSQL array contains 연산
                    areaPredicates.add(criteriaBuilder.isTrue(
                            criteriaBuilder.function("array_contains",
                                    Boolean.class,
                                    root.get("serviceAreas"),
                                    criteriaBuilder.literal(area))));
                }
                predicates.add(criteriaBuilder.or(areaPredicates.toArray(new Predicate[0])));
            }

            // 5. 태그 필터
            if (request.getTags() != null && request.getTags().length > 0) {
                List<Predicate> tagPredicates = new ArrayList<>();
                for (String tag : request.getTags()) {
                    // PostgreSQL array contains 연산
                    tagPredicates.add(criteriaBuilder.isTrue(
                            criteriaBuilder.function("array_contains",
                                    Boolean.class,
                                    root.get("tags"),
                                    criteriaBuilder.literal(tag))));
                }
                predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
            }

            // 6. 최소 평점 필터
            if (request.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("avgRating"), request.getMinRating()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
