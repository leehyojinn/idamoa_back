package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.model.CompanyReviewImage;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.repository.CompanyReviewImageRepository;
import com.hip.damoa.domain.company.repository.CompanyReviewRepository;
import com.hip.damoa.domain.company.web.dto.CompanyReviewCreateRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReviewService {

    private final CompanyReviewRepository reviewRepository;
    private final CompanyReviewImageRepository reviewImageRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * CompanyReview의 User로부터 userName 조회
     * UserProfile이 있으면 name 반환, 없으면 email 반환
     */
    private String getUserName(CompanyReview review) {
        if (review.getUser() == null) {
            return null;
        }

        return userProfileRepository.findByUserId(review.getUser().getId())
                .map(com.hip.damoa.domain.user.model.UserProfile::getName)
                .orElse(review.getUser().getEmail());
    }

    /**
     * 리뷰 작성
     */
    @Transactional
    public CompanyReview createReview(UUID companyUuid, String userEmail, CompanyReviewCreateRequest request) {
        log.info("리뷰 작성 시작: companyUuid={}, userEmail={}", companyUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 이미 리뷰를 작성한 경우 체크 (optional)
        // boolean exists = reviewRepository.existsByCompanyAndUser(company, user);
        // if (exists) {
        //     throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        // }

        // UUID 배열을 File ID 배열로 변환
        Long[] imageFileIds = convertUuidsToFileIds(request.getImageUuids());

        CompanyReview review = CompanyReview.builder()
                .company(company)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .images(imageFileIds)
                .status("PUBLISHED")
                .build();

        review = reviewRepository.save(review);

        // ✅ OneToMany 기반 이미지 처리 추가
        processReviewImages(review, request.getImageUuids());

        // 업체 평균 평점 업데이트
        updateCompanyRating(company);

        log.info("리뷰 작성 완료: reviewUuid={}", review.getId());
        return review;
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CompanyReview> getCompanyReviews(UUID companyUuid, Pageable pageable) {
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return reviewRepository.findByCompanyAndStatusOrderByCreatedAtDesc(company, "PUBLISHED", pageable);
    }

    /**
     * 리뷰 상세 조회
     */
    @Transactional(readOnly = true)
    public CompanyReview getReview(UUID reviewUuid) {
        return reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN)); // REVIEW_NOT_FOUND 에러코드 추가 필요
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public CompanyReview updateReview(UUID reviewUuid, String userEmail, CompanyReviewCreateRequest request) {
        log.info("리뷰 수정 시작: reviewUuid={}, userEmail={}", reviewUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 리뷰 작성자인지 확인
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // UUID 배열을 File ID 배열로 변환
        Long[] imageFileIds = convertUuidsToFileIds(request.getImageUuids());

        review.updateReview(request.getRating(), request.getTitle(), request.getContent(), imageFileIds);
        review = reviewRepository.save(review);

        // ✅ OneToMany 기반 이미지 업데이트 (기존 이미지 먼저 삭제)
        if (request.getImageUuids() != null) {
            // 즉시 삭제 (@Modifying with flushAutomatically)
            reviewImageRepository.deleteByReviewId(review.getId());
            processReviewImages(review, request.getImageUuids());
        }

        // 업체 평균 평점 업데이트
        updateCompanyRating(review.getCompany());

        log.info("리뷰 수정 완료: reviewUuid={}", reviewUuid);
        return review;
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(UUID reviewUuid, String userEmail) {
        log.info("리뷰 삭제 시작: reviewUuid={}, userEmail={}", reviewUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
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

        log.info("리뷰 삭제 완료: reviewUuid={}", reviewUuid);
    }

    /**
     * 업체 답변 작성
     */
    @Transactional
    public CompanyReview addReply(UUID reviewUuid, String userEmail, String reply) {
        log.info("업체 답변 작성 시작: reviewUuid={}, userEmail={}", reviewUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.addReply(reply);
        reviewRepository.save(review);

        log.info("업체 답변 작성 완료: reviewUuid={}", reviewUuid);
        return review;
    }

    /**
     * 업체 답변 수정
     */
    @Transactional
    public CompanyReview updateReply(UUID reviewUuid, String userEmail, String reply) {
        log.info("업체 답변 수정 시작: reviewUuid={}, userEmail={}", reviewUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.updateReply(reply);
        reviewRepository.save(review);

        log.info("업체 답변 수정 완료: reviewUuid={}", reviewUuid);
        return review;
    }

    /**
     * 업체 답변 삭제
     */
    @Transactional
    public void deleteReply(UUID reviewUuid, String userEmail) {
        log.info("업체 답변 삭제 시작: reviewUuid={}, userEmail={}", reviewUuid, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 업체 소유자인지 확인
        if (!review.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.deleteReply();
        reviewRepository.save(review);

        log.info("업체 답변 삭제 완료: reviewUuid={}", reviewUuid);
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

    /**
     * CompanyReview 엔티티를 Response DTO로 변환 (File ID → URL, UUID 변환 포함)
     */
    public com.hip.damoa.domain.company.web.dto.CompanyReviewResponse toResponse(CompanyReview review) {
        // ✅ OneToMany 기반 이미지 조회 사용 (UUID 포함)
        List<com.hip.damoa.domain.company.web.dto.ReviewImageDto> imageDtos = getReviewImageDtos(review);
        String userName = getUserName(review);
        return com.hip.damoa.domain.company.web.dto.CompanyReviewResponse.from(review, imageDtos, userName);
    }

    /**
     * 리뷰 이미지 처리 (Company 패턴 동일)
     * UUID 배열 → File ID로 변환 → company_review_images 테이블에 저장
     */
    private void processReviewImages(CompanyReview review, String[] imageUuids) {
        if (imageUuids == null || imageUuids.length == 0) {
            log.info("리뷰 이미지 없음: reviewId={}", review.getId());
            return;
        }

        log.info("리뷰 이미지 처리 시작: reviewId={}, imageCount={}", review.getId(), imageUuids.length);

        int displayOrder = 0;
        for (String imageUuid : imageUuids) {
            if (imageUuid != null && !imageUuid.isEmpty()) {
                // 1. files 테이블에서 UUID로 파일 조회
                UUID uuid = UUID.fromString(imageUuid);
                File file = fileRepository.findByUuidAndIsDeletedFalse(uuid)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

                // 2. files 테이블의 entity 정보 업데이트
                file.updateEntityInfo("REVIEW_IMAGE", review.getId());
                fileRepository.save(file);

                // 3. company_review_images 테이블에 File ID 저장
                CompanyReviewImage reviewImage = CompanyReviewImage.builder()
                        .review(review)
                        .fileId(file.getId())
                        .displayOrder(displayOrder++)
                        .build();

                reviewImageRepository.save(reviewImage);
            }
        }

        log.info("리뷰 이미지 저장 완료: reviewId={}, 저장된 이미지 수={}", review.getId(), displayOrder);
    }

    /**
     * 리뷰 이미지 조회 (File ID → URL 변환)
     */
    public List<String> getReviewImageUrls(CompanyReview review) {
        List<CompanyReviewImage> images = reviewImageRepository
                .findByReviewOrderByDisplayOrder(review);

        return images.stream()
                .map(image -> fileRepository.findById(image.getFileId())
                        .map(File::getFileUrl)
                        .orElse(null))
                .filter(url -> url != null)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 이미지 조회 (File ID → DTO 변환, UUID 포함)
     */
    public List<com.hip.damoa.domain.company.web.dto.ReviewImageDto> getReviewImageDtos(CompanyReview review) {
        List<CompanyReviewImage> images = reviewImageRepository
                .findByReviewOrderByDisplayOrder(review);

        return images.stream()
                .map(image -> {
                    File file = fileRepository.findById(image.getFileId()).orElse(null);
                    if (file == null) {
                        return null;
                    }
                    return com.hip.damoa.domain.company.web.dto.ReviewImageDto.from(
                            image,
                            file.getFileUrl(),
                            file.getUuid()
                    );
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * UUID 배열을 File ID 배열로 변환
     */
    private Long[] convertUuidsToFileIds(String[] uuidStrings) {
        if (uuidStrings == null || uuidStrings.length == 0) {
            return null;
        }

        return Arrays.stream(uuidStrings)
                .filter(uuidStr -> uuidStr != null && !uuidStr.isEmpty())
                .map(uuidStr -> {
                    UUID uuid = UUID.fromString(uuidStr);
                    return fileRepository.findByUuidAndIsDeletedFalse(uuid)
                            .map(File::getId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
                })
                .toArray(Long[]::new);
    }

    /**
     * File ID 배열을 URL 배열로 변환
     */
    private String[] convertFileIdsToUrls(Long[] fileIds) {
        if (fileIds == null || fileIds.length == 0) {
            return new String[0];
        }

        List<File> files = fileRepository.findAllById(Arrays.asList(fileIds));

        return Arrays.stream(fileIds)
                .map(id -> files.stream()
                        .filter(f -> f.getId().equals(id))
                        .findFirst()
                        .map(File::getFileUrl)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }
}
