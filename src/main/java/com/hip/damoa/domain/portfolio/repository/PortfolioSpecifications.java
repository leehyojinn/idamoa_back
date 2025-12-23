package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.portfolio.model.PortfolioBookmark;
import com.hip.damoa.domain.portfolio.model.PortfolioFilterOption;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

/**
 * CompanyPortfolio 엔티티에 대한 JPA Specification
 */
public class PortfolioSpecifications {

    /**
     * 삭제되지 않은 포트폴리오
     */
    public static Specification<CompanyPortfolio> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    /**
     * 공개된 포트폴리오
     */
    public static Specification<CompanyPortfolio> isPublic() {
        return (root, query, cb) -> cb.isTrue(root.get("isPublic"));
    }

    /**
     * 제목 또는 내용에서 키워드 검색
     */
    public static Specification<CompanyPortfolio> hasKeywordInTitleOrContent(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("content")), pattern)
            );
        };
    }

    /**
     * 태그에서 키워드 검색 (PostgreSQL 배열)
     */
    public static Specification<CompanyPortfolio> hasKeywordInTags(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // tags::text LIKE '%keyword%'
            return cb.like(
                cb.lower(
                    cb.function("text", String.class, root.get("tags"))
                ),
                pattern
            );
        };
    }

    /**
     * 업체 이름에서 키워드 검색
     */
    public static Specification<CompanyPortfolio> hasKeywordInCompanyName(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // CompanyPortfolio -> Company 조인
            Join<CompanyPortfolio, Company> companyJoin = root.join("company", JoinType.INNER);

            return cb.and(
                cb.isFalse(companyJoin.get("isDeleted")),
                cb.like(cb.lower(companyJoin.get("name")), pattern)
            );
        };
    }

    /**
     * 통합 키워드 검색 (제목, 내용, 태그, 업체명)
     */
    public static Specification<CompanyPortfolio> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // 제목, 설명, 내용
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
            Predicate contentMatch = cb.like(cb.lower(root.get("content")), pattern);

            // 태그 (배열을 텍스트로 변환)
            Predicate tagsMatch = cb.like(
                cb.lower(cb.function("text", String.class, root.get("tags"))),
                pattern
            );

            // 업체명 (서브쿼리 사용 - 조인은 페이징과 충돌할 수 있음)
            Subquery<Long> companySubquery = query.subquery(Long.class);
            Root<Company> companyRoot = companySubquery.from(Company.class);
            companySubquery.select(companyRoot.get("id"))
                .where(
                    cb.and(
                        cb.isFalse(companyRoot.get("isDeleted")),
                        cb.like(cb.lower(companyRoot.get("name")), pattern)
                    )
                );

            Predicate companyMatch = root.get("company").get("id").in(companySubquery);

            return cb.or(titleMatch, descMatch, contentMatch, tagsMatch, companyMatch);
        };
    }

    /**
     * 특정 업체의 포트폴리오
     */
    public static Specification<CompanyPortfolio> hasCompanyUuid(UUID companyUuid) {
        return (root, query, cb) -> {
            if (companyUuid == null) {
                return cb.conjunction();
            }

            Join<CompanyPortfolio, Company> companyJoin = root.join("company", JoinType.INNER);
            return cb.equal(companyJoin.get("uuid"), companyUuid);
        };
    }

    /**
     * 필터 옵션으로 검색 - JSONB 배열 방식
     */
    public static Specification<CompanyPortfolio> hasFilterOptions(List<Long> filterOptionIds) {
        return (root, query, cb) -> {
            if (filterOptionIds == null || filterOptionIds.isEmpty()) {
                return cb.conjunction();
            }

            // JSONB 배열에서 필터 옵션 중 하나라도 포함되어 있는지 확인 (OR 조건)
            Predicate[] predicates = filterOptionIds.stream()
                .map(optionId -> cb.isTrue(
                    cb.function("jsonb_contains_id",
                        Boolean.class,
                        root.get("filterOptionIds"),
                        cb.literal(optionId)
                    )
                ))
                .toArray(Predicate[]::new);

            return cb.or(predicates);
        };
    }

    /**
     * 특정 사용자가 북마크한 포트폴리오
     */
    public static Specification<CompanyPortfolio> isBookmarkedBy(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }

            query.distinct(true);

            // EXISTS 서브쿼리 사용
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<PortfolioBookmark> bookmarkRoot = subquery.from(PortfolioBookmark.class);

            subquery.select(bookmarkRoot.get("portfolio").get("id"))
                .where(
                    cb.equal(bookmarkRoot.get("portfolio").get("id"), root.get("id")),
                    cb.equal(bookmarkRoot.get("user").get("id"), userId)
                );

            return cb.exists(subquery);
        };
    }

    /**
     * 특정 사용자가 작성한 포트폴리오 (업체 소유자 기준)
     */
    public static Specification<CompanyPortfolio> isOwnedBy(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }

            Join<CompanyPortfolio, Company> companyJoin = root.join("company", JoinType.INNER);
            return cb.equal(companyJoin.get("owner").get("id"), userId);
        };
    }

    /**
     * 복합 검색 종합 메서드
     */
    public static Specification<CompanyPortfolio> search(
            String keyword,
            List<Long> filterOptionIds,
            UUID companyUuid,
            Boolean onlyBookmarked,
            Boolean onlyMyPosts,
            Long userId) {

        Specification<CompanyPortfolio> spec = Specification
            .where(isNotDeleted())
            .and(isPublic());

        // 특정 업체 필터
        if (companyUuid != null) {
            spec = spec.and(hasCompanyUuid(companyUuid));
        }

        // 키워드 통합 검색
        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(hasKeyword(keyword));
        }

        // 필터 옵션
        if (filterOptionIds != null && !filterOptionIds.isEmpty()) {
            spec = spec.and(hasFilterOptions(filterOptionIds));
        }

        // 북마크한 포트폴리오만
        if (Boolean.TRUE.equals(onlyBookmarked) && userId != null) {
            spec = spec.and(isBookmarkedBy(userId));
        }

        // 내 포트폴리오만
        if (Boolean.TRUE.equals(onlyMyPosts) && userId != null) {
            spec = spec.and(isOwnedBy(userId));
        }

        return spec;
    }
}
