package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.model.CompanyReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 업체 리뷰 이미지 Repository
 */
public interface CompanyReviewImageRepository extends JpaRepository<CompanyReviewImage, Long> {

    /**
     * 리뷰의 이미지 목록 조회 (display_order 순서로)
     */
    List<CompanyReviewImage> findByReviewOrderByDisplayOrder(CompanyReview review);

    /**
     * 리뷰의 모든 이미지 삭제 (즉시 실행)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CompanyReviewImage cri WHERE cri.review.id = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    /**
     * 리뷰의 모든 이미지 삭제
     */
    void deleteByReview(CompanyReview review);
}
