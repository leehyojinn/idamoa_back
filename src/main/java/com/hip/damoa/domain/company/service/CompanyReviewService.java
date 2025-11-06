package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.repository.CompanyReviewRepository;
import com.hip.damoa.domain.company.web.dto.CompanyReviewCreateRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReviewService {

    private final CompanyReviewRepository reviewRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public CompanyReview createReview(Long companyId, String userEmail, CompanyReviewCreateRequest request) {
        log.info("리뷰 작성 시작: companyId={}, userEmail={}", companyId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 이미 리뷰를 작성한 경우 체크 (optional)
        // boolean exists = reviewRepository.existsByCompanyAndUser(company, user);
        // if (exists) {
        //     throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        // }

        CompanyReview review = CompanyReview.builder()
                .company(company)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .images(request.getImages())
                .status("PUBLISHED")
                .build();

        review = reviewRepository.save(review);

        // 업체 평균 평점 업데이트
        updateCompanyRating(company);

        log.info("리뷰 작성 완료: reviewId={}", review.getId());
        return review;
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CompanyReview> getCompanyReviews(Long companyId, Pageable pageable) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return reviewRepository.findByCompanyAndStatusOrderByCreatedAtDesc(company, "PUBLISHED", pageable);
    }

    /**
     * 리뷰 상세 조회
     */
    @Transactional(readOnly = true)
    public CompanyReview getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN)); // REVIEW_NOT_FOUND 에러코드 추가 필요
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public CompanyReview updateReview(Long reviewId, String userEmail, CompanyReviewCreateRequest request) {
        log.info("리뷰 수정 시작: reviewId={}, userEmail={}", reviewId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 리뷰 작성자인지 확인
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.updateReview(request.getRating(), request.getTitle(), request.getContent(), request.getImages());
        review = reviewRepository.save(review);

        // 업체 평균 평점 업데이트
        updateCompanyRating(review.getCompany());

        log.info("리뷰 수정 완료: reviewId={}", reviewId);
        return review;
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        log.info("리뷰 삭제 시작: reviewId={}, userEmail={}", reviewId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 리뷰 작성자인지 확인
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Company company = review.getCompany();
        review.softDelete();
        reviewRepository.save(review);

        // 업체 평균 평점 업데이트
        updateCompanyRating(company);

        log.info("리뷰 삭제 완료: reviewId={}", reviewId);
    }

    /**
     * 업체 답변 작성
     */
    @Transactional
    public CompanyReview addReply(Long reviewId, String userEmail, String reply) {
        log.info("업체 답변 작성 시작: reviewId={}, userEmail={}", reviewId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.addReply(reply);
        reviewRepository.save(review);

        log.info("업체 답변 작성 완료: reviewId={}", reviewId);
        return review;
    }

    /**
     * 업체 답변 수정
     */
    @Transactional
    public CompanyReview updateReply(Long reviewId, String userEmail, String reply) {
        log.info("업체 답변 수정 시작: reviewId={}, userEmail={}", reviewId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.updateReply(reply);
        reviewRepository.save(review);

        log.info("업체 답변 수정 완료: reviewId={}", reviewId);
        return review;
    }

    /**
     * 업체 답변 삭제
     */
    @Transactional
    public void deleteReply(Long reviewId, String userEmail) {
        log.info("업체 답변 삭제 시작: reviewId={}, userEmail={}", reviewId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.deleteReply();
        reviewRepository.save(review);

        log.info("업체 답변 삭제 완료: reviewId={}", reviewId);
    }

    /**
     * 업체 평균 평점 업데이트
     */
    private void updateCompanyRating(Company company) {
        List<CompanyReview> reviews = reviewRepository.findByCompanyAndStatus(company, "PUBLISHED");

        if (reviews.isEmpty()) {
            company.updateRating(BigDecimal.ZERO, 0);
        } else {
            BigDecimal totalRating = reviews.stream()
                    .map(CompanyReview::getRating)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgRating = totalRating.divide(
                    BigDecimal.valueOf(reviews.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            company.updateRating(avgRating, reviews.size());
        }

        companyRepository.save(company);
    }
}
