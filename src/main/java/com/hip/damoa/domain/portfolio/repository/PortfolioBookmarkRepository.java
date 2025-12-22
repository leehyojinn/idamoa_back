package com.hip.damoa.domain.portfolio.repository;

import com.hip.damoa.domain.portfolio.model.PortfolioBookmark;
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
public interface PortfolioBookmarkRepository extends JpaRepository<PortfolioBookmark, Long> {

    /**
     * 특정 북마크 조회
     */
    Optional<PortfolioBookmark> findByPortfolioIdAndUserId(Long portfolioId, Long userId);

    /**
     * 사용자의 북마크 목록 조회 (페이징, 삭제된 포트폴리오 제외)
     */
    @Query("SELECT pb FROM PortfolioBookmark pb " +
           "JOIN pb.portfolio p " +
           "WHERE pb.user.id = :userId " +
           "AND p.isDeleted = false " +
           "ORDER BY pb.createdAt DESC")
    Page<PortfolioBookmark> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자의 북마크 목록 조회 (전체, 삭제된 포트폴리오 제외)
     */
    @Query("SELECT pb FROM PortfolioBookmark pb " +
           "JOIN pb.portfolio p " +
           "WHERE pb.user.id = :userId " +
           "AND p.isDeleted = false " +
           "ORDER BY pb.createdAt DESC")
    List<PortfolioBookmark> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자가 북마크한 포트폴리오 ID 목록 조회 (삭제된 포트폴리오 제외)
     */
    @Query("SELECT pb.portfolio.id FROM PortfolioBookmark pb " +
           "JOIN pb.portfolio p " +
           "WHERE pb.user.id = :userId " +
           "AND p.isDeleted = false")
    List<Long> findPortfolioIdsByUserId(@Param("userId") Long userId);

    /**
     * 북마크 여부 확인
     */
    boolean existsByPortfolioIdAndUserId(Long portfolioId, Long userId);

    /**
     * 북마크 삭제
     */
    @Modifying
    @Query("DELETE FROM PortfolioBookmark pb WHERE pb.portfolio.id = :portfolioId AND pb.user.id = :userId")
    void deleteByPortfolioIdAndUserId(@Param("portfolioId") Long portfolioId, @Param("userId") Long userId);

    /**
     * 포트폴리오의 북마크 수 조회
     */
    long countByPortfolioId(Long portfolioId);
}
