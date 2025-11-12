package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.BoardCategory;
import com.hip.damoa.domain.board.model.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    // Find by board type
    List<BoardCategory> findByBoardType(BoardType boardType);

    // Find by board type ordered by display order
    @Query("SELECT c FROM BoardCategory c WHERE c.boardType = :boardType " +
           "ORDER BY c.displayOrder ASC")
    List<BoardCategory> findByBoardTypeOrderByDisplayOrder(@Param("boardType") BoardType boardType);

    // Find by category name
    Optional<BoardCategory> findByName(String name);

    // Find by board type and category name
    Optional<BoardCategory> findByBoardTypeAndName(BoardType boardType, String name);

    // Find active categories by board type
    @Query("SELECT c FROM BoardCategory c WHERE c.boardType = :boardType " +
           "AND c.isActive = true " +
           "ORDER BY c.displayOrder ASC")
    List<BoardCategory> findActiveByBoardType(@Param("boardType") BoardType boardType);

    // Count by board type
    long countByBoardType(BoardType boardType);

    // Count active by board type
    @Query("SELECT COUNT(c) FROM BoardCategory c WHERE c.boardType = :boardType " +
           "AND c.isActive = true")
    long countActiveByBoardType(@Param("boardType") BoardType boardType);

    // Check if category name exists in board type
    boolean existsByBoardTypeAndName(BoardType boardType, String name);
}
