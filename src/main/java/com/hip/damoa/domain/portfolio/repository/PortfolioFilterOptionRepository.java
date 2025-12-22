package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioFilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioFilterOptionRepository extends JpaRepository<PortfolioFilterOption, Long> {

    /**
     * 포트폴리오의 모든 필터 옵션 조회
     */
    List<PortfolioFilterOption> findByPortfolioId(Long portfolioId);

    /**
     * 필터 옵션이 적용된 포트폴리오 ID 목록 조회
     */
    @Query("SELECT pfo.portfolio.id FROM PortfolioFilterOption pfo WHERE pfo.filterOption.id = :filterOptionId")
    List<Long> findPortfolioIdsByFilterOptionId(@Param("filterOptionId") Long filterOptionId);

    /**
     * 여러 필터 옵션이 모두 적용된 포트폴리오 ID 목록 조회
     */
    @Query("SELECT pfo.portfolio.id FROM PortfolioFilterOption pfo " +
           "WHERE pfo.filterOption.id IN :filterOptionIds " +
           "GROUP BY pfo.portfolio.id " +
           "HAVING COUNT(DISTINCT pfo.filterOption.id) = :count")
    List<Long> findPortfolioIdsByAllFilterOptionIds(
            @Param("filterOptionIds") List<Long> filterOptionIds,
            @Param("count") long count);

    /**
     * 포트폴리오의 필터 옵션 삭제
     */
    @Modifying
    @Query("DELETE FROM PortfolioFilterOption pfo WHERE pfo.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오와 필터 옵션 조합 존재 여부 확인
     */
    boolean existsByPortfolioIdAndFilterOptionId(Long portfolioId, Long filterOptionId);
}
