package com.hip.damoa.domain.board.repository;

import com.hip.damoa.domain.board.model.BoardFilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardFilterOptionRepository extends JpaRepository<BoardFilterOption, Long> {

    // Board별 필터 옵션 조회
    List<BoardFilterOption> findByBoardId(Long boardId);

    // FilterOption별 사용 조회
    List<BoardFilterOption> findByFilterOptionId(Long filterOptionId);

    // 특정 Board-FilterOption 존재 여부
    boolean existsByBoardIdAndFilterOptionId(Long boardId, Long filterOptionId);

    // Board의 모든 필터 옵션 삭제
    @Modifying
    @Query("DELETE FROM BoardFilterOption bfo WHERE bfo.board.id = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);

    // 특정 FilterOption 삭제
    @Modifying
    @Query("DELETE FROM BoardFilterOption bfo WHERE bfo.board.id = :boardId AND bfo.filterOption.id = :filterOptionId")
    void deleteByBoardIdAndFilterOptionId(@Param("boardId") Long boardId, @Param("filterOptionId") Long filterOptionId);

    // FilterOption 사용 횟수 카운트
    long countByFilterOptionId(Long filterOptionId);

    // [N+1 최적화] Board ID 목록으로 FilterOption 일괄 조회
    @Query("SELECT bfo FROM BoardFilterOption bfo " +
           "JOIN FETCH bfo.filterOption fo " +
           "JOIN FETCH fo.category " +
           "WHERE bfo.board.id IN :boardIds")
    List<BoardFilterOption> findByBoardIdIn(@Param("boardIds") List<Long> boardIds);

    // Board의 특정 카테고리 필터 조회
    @Query("SELECT bfo FROM BoardFilterOption bfo " +
           "JOIN bfo.filterOption fo " +
           "JOIN fo.category fc " +
           "WHERE bfo.board.id = :boardId AND fc.code = :categoryCode")
    List<BoardFilterOption> findByBoardIdAndCategoryCode(@Param("boardId") Long boardId,
                                                          @Param("categoryCode") String categoryCode);
}
