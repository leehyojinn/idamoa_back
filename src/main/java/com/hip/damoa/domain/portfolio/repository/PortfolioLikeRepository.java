package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioLikeRepository extends JpaRepository<PortfolioLike, Long> {

    /**
     * 특정 좋아요 조회
     */
    Optional<PortfolioLike> findByPortfolioIdAndUserId(Long portfolioId, Long userId);

    /**
     * 사용자의 좋아요 목록 조회 (페이징, 삭제된 포트폴리오 제외)
     */
    @Query("SELECT pl FROM PortfolioLike pl " +
           "JOIN pl.portfolio p " +
           "WHERE pl.user.id = :userId " +
           "AND p.isDeleted = false " +
           "ORDER BY pl.createdAt DESC")
    Page<PortfolioLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자의 좋아요 목록 조회 (전체, 삭제된 포트폴리오 제외)
     */
    @Query("SELECT pl FROM PortfolioLike pl " +
           "JOIN pl.portfolio p " +
           "WHERE pl.user.id = :userId " +
           "AND p.isDeleted = false " +
           "ORDER BY pl.createdAt DESC")
    List<PortfolioLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자가 좋아요한 포트폴리오 ID 목록 조회 (삭제된 포트폴리오 제외)
     */
    @Query("SELECT pl.portfolio.id FROM PortfolioLike pl " +
           "JOIN pl.portfolio p " +
           "WHERE pl.user.id = :userId " +
           "AND p.isDeleted = false")
    List<Long> findPortfolioIdsByUserId(@Param("userId") Long userId);

    /**
     * 좋아요 여부 확인
     */
    boolean existsByPortfolioIdAndUserId(Long portfolioId, Long userId);

    /**
     * 좋아요 삭제
     */
    @Modifying
    @Query("DELETE FROM PortfolioLike pl WHERE pl.portfolio.id = :portfolioId AND pl.user.id = :userId")
    void deleteByPortfolioIdAndUserId(@Param("portfolioId") Long portfolioId, @Param("userId") Long userId);

    /**
     * 포트폴리오의 좋아요 수 조회
     */
    long countByPortfolioId(Long portfolioId);
}
