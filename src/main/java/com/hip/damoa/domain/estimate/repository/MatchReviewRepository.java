package com.hip.damoa.domain.estimate.repository;

import com.hip.damoa.domain.estimate.model.Match;
import com.hip.damoa.domain.estimate.model.MatchReview;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchReviewRepository extends JpaRepository<MatchReview, Long> {

    // Find by match
    List<MatchReview> findByMatch(Match match);

    // Find by match and reviewer type
    @Query("SELECT r FROM MatchReview r WHERE r.match = :match " +
           "AND r.reviewerType = :reviewerType")
    Optional<MatchReview> findByMatchAndReviewerType(@Param("match") Match match,
                                                       @Param("reviewerType") String reviewerType);

    // Find by reviewer
    List<MatchReview> findByReviewer(User reviewer);

    Page<MatchReview> findByReviewer(User reviewer, Pageable pageable);

    // Find by reviewer type
    List<MatchReview> findByReviewerType(String reviewerType);

    Page<MatchReview> findByReviewerType(String reviewerType, Pageable pageable);

    // Find by status
    List<MatchReview> findByStatus(String status);

    Page<MatchReview> findByStatus(String status, Pageable pageable);

    // Find published reviews
    @Query("SELECT r FROM MatchReview r WHERE r.status = 'PUBLISHED' " +
           "ORDER BY r.createdAt DESC")
    Page<MatchReview> findPublishedReviews(Pageable pageable);

    // Find by rating range
    @Query("SELECT r FROM MatchReview r WHERE r.rating >= :minRating " +
           "AND r.rating <= :maxRating " +
           "AND r.status = 'PUBLISHED'")
    List<MatchReview> findByRatingRange(@Param("minRating") BigDecimal minRating,
                                         @Param("maxRating") BigDecimal maxRating);

    // Find high rated reviews
    @Query("SELECT r FROM MatchReview r WHERE r.rating >= :minRating " +
           "AND r.status = 'PUBLISHED' " +
           "ORDER BY r.rating DESC, r.createdAt DESC")
    Page<MatchReview> findHighRatedReviews(@Param("minRating") BigDecimal minRating,
                                            Pageable pageable);

    // Find recommended reviews
    @Query("SELECT r FROM MatchReview r WHERE r.recommend = true " +
           "AND r.status = 'PUBLISHED' " +
           "ORDER BY r.createdAt DESC")
    Page<MatchReview> findRecommendedReviews(Pageable pageable);

    // Find most liked reviews
    @Query("SELECT r FROM MatchReview r WHERE r.status = 'PUBLISHED' " +
           "ORDER BY r.likeCount DESC")
    Page<MatchReview> findMostLikedReviews(Pageable pageable);

    // Calculate average rating by reviewer type
    @Query("SELECT AVG(r.rating) FROM MatchReview r " +
           "WHERE r.reviewerType = :reviewerType AND r.status = 'PUBLISHED'")
    BigDecimal calculateAverageRatingByReviewerType(@Param("reviewerType") String reviewerType);

    // Count by match
    long countByMatch(Match match);

    // Count by reviewer
    long countByReviewer(User reviewer);

    // Count by status
    long countByStatus(String status);

    // Count recommended reviews
    long countByRecommendTrue();

    // Check if review exists
    boolean existsByMatchAndReviewer(Match match, User reviewer);
}
