package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    // Find by board
    List<BoardCategory> findByBoard(Board board);

    // Find by board ordered by display order
    @Query("SELECT c FROM BoardCategory c WHERE c.board = :board " +
           "ORDER BY c.displayOrder ASC")
    List<BoardCategory> findByBoardOrderByDisplayOrder(@Param("board") Board board);

    // Find by category name
    Optional<BoardCategory> findByCategoryName(String categoryName);

    // Find by board and category name
    Optional<BoardCategory> findByBoardAndCategoryName(Board board, String categoryName);

    // Find active categories by board
    @Query("SELECT c FROM BoardCategory c WHERE c.board = :board " +
           "AND c.isActive = true " +
           "ORDER BY c.displayOrder ASC")
    List<BoardCategory> findActiveBoardAndByBoard(@Param("board") Board board);

    // Count by board
    long countByBoard(Board board);

    // Count active by board
    @Query("SELECT COUNT(c) FROM BoardCategory c WHERE c.board = :board " +
           "AND c.isActive = true")
    long countActiveByBoard(@Param("board") Board board);

    // Check if category name exists in board
    boolean existsByBoardAndCategoryName(Board board, String categoryName);
}
