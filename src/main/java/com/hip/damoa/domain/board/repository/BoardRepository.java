package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByBoardCode(String boardCode);

    List<Board> findByBoardType(String boardType);

    List<Board> findByIsActiveTrue();

    List<Board> findByBoardTypeAndIsActiveTrue(String boardType);

    boolean existsByBoardCode(String boardCode);
}
