package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // UUID 조회
    Optional<Board> findByUuidAndIsDeletedFalse(UUID uuid);

    Optional<Board> findByUuid(UUID uuid);

    // 게시판 타입별 조회
    Page<Board> findByBoardTypeAndIsDeletedFalseAndIsPublishedTrue(
            String boardType, Pageable pageable);

    Page<Board> findByBoardTypeAndIsDeletedFalse(String boardType, Pageable pageable);

    List<Board> findByBoardTypeAndIsDeletedFalseAndIsPublishedTrue(String boardType);

    // 게시판 여러 타입 조회 (NOTICE, EVENT, FAQ 통합 조회용)
    Page<Board> findByBoardTypeInAndIsDeletedFalseAndIsPublishedTrue(
            List<String> boardTypes, Pageable pageable);

    List<Board> findByBoardTypeInAndIsPinnedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc(
            List<String> boardTypes);

    // 카테고리별 조회
    Page<Board> findByCategoryIdAndIsDeletedFalseAndIsPublishedTrue(
            Long categoryId, Pageable pageable);

    // 사용자별 조회
    Page<Board> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    List<Board> findByUserIdAndBoardTypeAndIsDeletedFalse(Long userId, String boardType);

    // Featured 게시글
    List<Board> findByIsFeaturedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc();

    // Pinned 게시글
    List<Board> findByBoardTypeAndIsPinnedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc(
            String boardType);

    // 검색
    @Query("SELECT b FROM Board b WHERE b.boardType = :boardType " +
           "AND b.isDeleted = false AND b.isPublished = true " +
           "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Board> searchByKeyword(@Param("boardType") String boardType,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    // 여러 타입 검색
    @Query("SELECT b FROM Board b WHERE b.boardType IN :boardTypes " +
           "AND b.isDeleted = false AND b.isPublished = true " +
           "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Board> searchByKeywordAndBoardTypes(@Param("boardTypes") List<String> boardTypes,
                                             @Param("keyword") String keyword,
                                             Pageable pageable);

    // 북마크된 게시글 조회
    @Query("SELECT DISTINCT b FROM Board b " +
           "JOIN b.bookmarks bb " +
           "WHERE b.boardType = :boardType " +
           "AND b.isDeleted = false AND b.isPublished = true " +
           "AND bb.user.email = :userEmail")
    Page<Board> findBookmarkedBoards(@Param("boardType") String boardType,
                                     @Param("userEmail") String userEmail,
                                     Pageable pageable);

    // 필터 옵션으로 게시글 조회
    @Query("SELECT DISTINCT b FROM Board b " +
           "JOIN b.filterOptions bfo " +
           "WHERE b.boardType = :boardType " +
           "AND b.isDeleted = false AND b.isPublished = true " +
           "AND bfo.filterOption.id IN :filterOptionIds")
    Page<Board> findByFilterOptions(@Param("boardType") String boardType,
                                    @Param("filterOptionIds") List<Long> filterOptionIds,
                                    Pageable pageable);

    // 통계
    long countByBoardTypeAndIsDeletedFalse(String boardType);

    long countByUserIdAndIsDeletedFalse(Long userId);

    // 존재 여부
    boolean existsByUuidAndIsDeletedFalse(UUID uuid);
}

