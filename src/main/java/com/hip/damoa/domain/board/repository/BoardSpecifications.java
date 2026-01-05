package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardBookmark;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Board 엔티티에 대한 JPA Specification을 정의하는 클래스
 * CompanySpecifications와 동일한 패턴으로 구현
 */
public class BoardSpecifications {

    /**
     * 특정 게시판 타입 필터
     */
    public static Specification<Board> hasBoardType(String boardType) {
        return (root, query, cb) -> {
            if (boardType == null || boardType.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("boardType"), boardType);
        };
    }

    /**
     * 삭제되지 않은 게시글
     */
    public static Specification<Board> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    /**
     * 공개된 게시글
     */
    public static Specification<Board> isPublished() {
        return (root, query, cb) -> cb.isTrue(root.get("isPublished"));
    }

    /**
     * 키워드 검색 (제목, 내용, 태그에서 검색)
     */
    public static Specification<Board> hasKeywordInTitleOrContent(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // 제목 또는 내용에서 검색
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("content")), pattern)
            );
        };
    }

    /**
     * 태그에서 키워드 검색 (PostgreSQL 배열 검색)
     * tags text[] 필드에서 키워드를 포함하는 태그 검색
     */
    public static Specification<Board> hasKeywordInTags(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            // PostgreSQL의 텍스트 검색 연산자 사용
            // tags::text LIKE '%keyword%'
            // 배열을 텍스트로 캐스팅하면 {tag1,tag2,tag3} 형식이 됨
            String pattern = "%" + keyword.toLowerCase() + "%";

            // SQL: LOWER(CAST(tags AS text)) LIKE '%keyword%'
            return cb.like(
                cb.lower(
                    cb.function("text", String.class, root.get("tags"))
                ),
                pattern
            );
        };
    }

    /**
     * 회사 이름에서 키워드 검색
     * Board → User → Company (owner) 관계를 통해 회사 이름 검색
     */
    public static Specification<Board> hasKeywordInCompanyName(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // 서브쿼리: Board.user.id = Company.owner.id 인 Company 중 name이 keyword 포함
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Company> companyRoot = subquery.from(Company.class);

            subquery.select(companyRoot.get("owner").get("id"))
                .where(
                    cb.and(
                        cb.isNotNull(companyRoot.get("owner")),
                        cb.isFalse(companyRoot.get("isDeleted")),
                        cb.like(cb.lower(companyRoot.get("name")), pattern)
                    )
                );

            // Board.user.id IN (회사 owner ids)
            return cb.and(
                cb.isNotNull(root.get("user")),
                root.get("user").get("id").in(subquery)
            );
        };
    }

    /**
     * 키워드 통합 검색 (제목, 내용, 태그에서 검색)
     */
    public static Specification<Board> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            // 제목 또는 내용에서 검색
            Predicate titleOrContent = cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("content")), pattern)
            );

            // 태그에서 검색 (배열을 텍스트로 변환하여 검색)
            Predicate tags = cb.like(
                cb.lower(cb.function("text", String.class, root.get("tags"))),
                pattern
            );

            // 제목, 내용, 태그 중 하나라도 일치하면 검색
            return cb.or(titleOrContent, tags);
        };
    }

    /**
     * 필터 옵션으로 검색 - JSONB 배열 방식
     * filter_option_ids JSONB 배열에서 검색
     */
    public static Specification<Board> hasFilterOptions(List<Long> filterOptionIds) {
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
     * 특정 사용자가 북마크한 게시글
     */
    public static Specification<Board> isBookmarkedBy(String userEmail) {
        return (root, query, cb) -> {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return cb.conjunction();
            }

            // DISTINCT 설정
            query.distinct(true);

            // BoardBookmark와 JOIN
            Join<Board, BoardBookmark> bookmarkJoin = root.join("bookmarks", JoinType.INNER);
            Join<BoardBookmark, User> userJoin = bookmarkJoin.join("user", JoinType.INNER);

            // bookmark.user.email = userEmail
            return cb.equal(userJoin.get("email"), userEmail);
        };
    }

    /**
     * 특정 사용자가 작성한 게시글 (내글만 보기)
     */
    public static Specification<Board> isWrittenBy(String userEmail) {
        return (root, query, cb) -> {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return cb.conjunction();
            }

            // User와 JOIN
            Join<Board, User> userJoin = root.join("user", JoinType.INNER);

            // board.user.email = userEmail
            return cb.equal(userJoin.get("email"), userEmail);
        };
    }

    /**
     * 필터 옵션 고급 검색 - JSONB 배열 방식
     * CompanySpecifications와 동일한 패턴
     */
    public static Specification<Board> hasFilterOptionsAdvanced(List<Long> filterOptionIds) {
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
     * 특정 태그들을 포함하는 게시글 필터 (정확히 일치)
     */
    public static Specification<Board> hasTags(String[] tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.length == 0) {
                return cb.conjunction();
            }

            // 여러 태그 중 하나라도 포함하는 게시글 (OR 조건)
            // PostgreSQL: tags && ARRAY['tag1','tag2']::text[]
            // 배열을 텍스트로 변환하여 LIKE로 검색
            Predicate[] predicates = new Predicate[tags.length];
            for (int i = 0; i < tags.length; i++) {
                String tag = tags[i].toLowerCase();
                predicates[i] = cb.like(
                    cb.lower(
                        cb.function("text", String.class, root.get("tags"))
                    ),
                    "%{%" + tag + "%}%"  // {tag1,tag2} 형식에서 검색
                );
            }

            return cb.or(predicates);
        };
    }

    /**
     * 특정 회사의 게시글 필터 (companyUuid로 필터링)
     * Company.owner.id = Board.user.id 관계로 필터링
     */
    public static Specification<Board> hasCompanyUuid(java.util.UUID companyUuid) {
        return (root, query, cb) -> {
            if (companyUuid == null) {
                return cb.conjunction();
            }

            // 서브쿼리: companyUuid로 Company 찾고 owner.id 가져오기
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Company> companyRoot = subquery.from(Company.class);
            subquery.select(companyRoot.get("owner").get("id"))
                .where(
                    cb.and(
                        cb.equal(companyRoot.get("uuid"), companyUuid),
                        cb.isNotNull(companyRoot.get("owner")),
                        cb.isFalse(companyRoot.get("isDeleted"))
                    )
                );

            // Board.user.id = Company.owner.id
            return cb.and(
                cb.isNotNull(root.get("user")),
                root.get("user").get("id").in(subquery)
            );
        };
    }

    /**
     * 복합 검색을 위한 종합 메서드 (tags 파라미터 없는 버전 - 하위 호환성)
     */
    public static Specification<Board> searchBoards(
            String boardType,
            String keyword,
            List<Long> filterOptionIds,
            Boolean onlyBookmarked,
            Boolean onlyMyPosts,
            String userEmail) {
        return searchBoards(boardType, keyword, null, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail, null);
    }

    /**
     * 복합 검색을 위한 종합 메서드 (tags 포함, companyUuid 없는 버전 - 하위 호환성)
     */
    public static Specification<Board> searchBoards(
            String boardType,
            String keyword,
            String[] tags,
            List<Long> filterOptionIds,
            Boolean onlyBookmarked,
            Boolean onlyMyPosts,
            String userEmail) {
        return searchBoards(boardType, keyword, tags, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail, null);
    }

    /**
     * 복합 검색을 위한 종합 메서드
     * 모든 조건을 AND로 결합
     */
    public static Specification<Board> searchBoards(
            String boardType,
            String keyword,
            String[] tags,
            List<Long> filterOptionIds,
            Boolean onlyBookmarked,
            Boolean onlyMyPosts,
            String userEmail,
            java.util.UUID companyUuid) {

        Specification<Board> spec = Specification.where(isNotDeleted())
            .and(isPublished())
            .and(hasBoardType(boardType));

        // 회사 필터 (companyUuid)
        if (companyUuid != null) {
            spec = spec.and(hasCompanyUuid(companyUuid));
        }

        // 키워드 검색 (제목, 내용, 태그, 회사 이름)
        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(hasKeyword(keyword));
        }

        // 태그 필터 (정확한 태그 매칭)
        if (tags != null && tags.length > 0) {
            spec = spec.and(hasTags(tags));
        }

        // 필터 옵션
        if (filterOptionIds != null && !filterOptionIds.isEmpty()) {
            spec = spec.and(hasFilterOptions(filterOptionIds));
        }

        // 북마크한 글만
        if (Boolean.TRUE.equals(onlyBookmarked) && userEmail != null) {
            spec = spec.and(isBookmarkedBy(userEmail));
        }

        // 내가 쓴 글만
        if (Boolean.TRUE.equals(onlyMyPosts) && userEmail != null) {
            spec = spec.and(isWrittenBy(userEmail));
        }

        return spec;
    }
}