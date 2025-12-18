package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    // 특정 사용자-게시글 좋아요 조회
    Optional<BoardLike> findByBoardIdAndUserId(Long boardId, Long userId);

    // 좋아요 존재 여부
    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    // 게시글의 좋아요 수
    long countByBoardId(Long boardId);

    // 삭제
    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardId(Long boardId);

    // [N+1 최적화] 사용자가 좋아요한 Board ID 목록 일괄 조회
    @Query("SELECT bl.board.id FROM BoardLike bl " +
           "WHERE bl.board.id IN :boardIds AND bl.user.id = :userId")
    List<Long> findLikedBoardIds(@Param("boardIds") List<Long> boardIds, @Param("userId") Long userId);
}
