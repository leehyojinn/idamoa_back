package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.model.CompanyReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 리뷰의 모든 이미지 삭제
     */
    void deleteByReview(CompanyReview review);
}
