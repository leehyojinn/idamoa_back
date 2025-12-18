package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.BoardBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {

    // 사용자의 북마크 조회
    Page<BoardBookmark> findByUserId(Long userId, Pageable pageable);

    List<BoardBookmark> findByUserId(Long userId);

    // 게시글의 북마크 목록
    List<BoardBookmark> findByBoardId(Long boardId);

    // 특정 사용자-게시글 북마크 조회
    Optional<BoardBookmark> findByBoardIdAndUserId(Long boardId, Long userId);

    // 북마크 존재 여부
    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    // 게시글의 북마크 수
    long countByBoardId(Long boardId);

    // 사용자의 북마크 수
    long countByUserId(Long userId);

    // 사용자의 특정 타입 북마크 조회
    @Query("SELECT bb FROM BoardBookmark bb " +
           "JOIN bb.board b " +
           "WHERE bb.user.id = :userId AND b.boardType = :boardType " +
           "AND b.isDeleted = false AND b.isPublished = true " +
           "ORDER BY bb.createdAt DESC")
    Page<BoardBookmark> findByUserIdAndBoardType(@Param("userId") Long userId,
                                                  @Param("boardType") String boardType,
                                                  Pageable pageable);

    // 삭제
    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardId(Long boardId);

    // [N+1 최적화] 사용자가 북마크한 Board ID 목록 일괄 조회
    @Query("SELECT bb.board.id FROM BoardBookmark bb " +
           "WHERE bb.board.id IN :boardIds AND bb.user.id = :userId")
    List<Long> findBookmarkedBoardIds(@Param("boardIds") List<Long> boardIds, @Param("userId") Long userId);
}
