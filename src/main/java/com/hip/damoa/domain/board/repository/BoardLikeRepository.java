package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
