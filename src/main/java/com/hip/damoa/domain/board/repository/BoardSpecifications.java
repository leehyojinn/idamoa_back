package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardBookmark;
import com.hip.damoa.domain.board.model.BoardFilterOption;
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

            // PostgreSQL의 text[] 배열에서 특정 텍스트 포함 여부 확인
            // SQL: keyword = ANY(tags) OR EXISTS (SELECT 1 FROM unnest(tags) t WHERE t ILIKE '%keyword%')

            // 정확히 일치하는 태그 검색
            Predicate exactMatch = cb.isNotNull(
                cb.function("array_position", Integer.class,
                    root.get("tags"),
                    cb.literal(keyword)
                )
            );

            // 부분 일치하는 태그 검색 (ILIKE 사용)
            // PostgreSQL 함수 사용: array_to_string(tags, ',') ILIKE '%keyword%'
            Predicate partialMatch = cb.like(
                cb.lower(
                    cb.function("array_to_string", String.class,
                        root.get("tags"),
                        cb.literal(",")
                    )
                ),
                "%" + keyword.toLowerCase() + "%"
            );

            return cb.or(exactMatch, partialMatch);
        };
    }

    /**
     * 키워드 통합 검색 (제목, 내용, 태그 모두에서 검색)
     */
    public static Specification<Board> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            return Specification
                .where(hasKeywordInTitleOrContent(keyword))
                .or(hasKeywordInTags(keyword))
                .toPredicate(root, query, cb);
        };
    }

    /**
     * 필터 옵션으로 검색
     * BoardFilterOption 조인 테이블을 통해 필터링
     */
    public static Specification<Board> hasFilterOptions(List<Long> filterOptionIds) {
        return (root, query, cb) -> {
            if (filterOptionIds == null || filterOptionIds.isEmpty()) {
                return cb.conjunction();
            }

            // DISTINCT 설정
            query.distinct(true);

            // BoardFilterOption과 JOIN
            Join<Board, BoardFilterOption> filterJoin = root.join("filterOptions", JoinType.INNER);

            // filterOption.id IN (filterOptionIds)
            return filterJoin.get("filterOption").get("id").in(filterOptionIds);
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
     * 필터 옵션 고급 검색 (카테고리별 OR, 카테고리간 AND)
     * CompanySpecifications와 유사한 패턴
     */
    public static Specification<Board> hasFilterOptionsAdvanced(List<Long> filterOptionIds) {
        return (root, query, cb) -> {
            if (filterOptionIds == null || filterOptionIds.isEmpty()) {
                return cb.conjunction();
            }

            // DISTINCT 설정
            query.distinct(true);

            // EXISTS 서브쿼리 사용
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BoardFilterOption> bfoRoot = subquery.from(BoardFilterOption.class);

            subquery.select(bfoRoot.get("board").get("id"))
                .where(
                    cb.equal(bfoRoot.get("board").get("id"), root.get("id")),
                    bfoRoot.get("filterOption").get("id").in(filterOptionIds)
                );

            return cb.exists(subquery);
        };
    }

    /**
     * 복합 검색을 위한 종합 메서드
     * 모든 조건을 AND로 결합
     */
    public static Specification<Board> searchBoards(
            String boardType,
            String keyword,
            List<Long> filterOptionIds,
            Boolean onlyBookmarked,
            Boolean onlyMyPosts,
            String userEmail) {

        Specification<Board> spec = Specification.where(isNotDeleted())
            .and(isPublished())
            .and(hasBoardType(boardType));

        // 키워드 검색 (제목, 내용, 태그)
        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(hasKeyword(keyword));
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