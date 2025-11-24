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
import com.hip.damoa.domain.company.web.dto.CompanyReviewResponse;
import com.hip.damoa.domain.company.web.dto.ReviewImageDto;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;

    /**
     * CompanyReview의 User로부터 userName 조회
     * UserProfile이 있으면 name 반환, 없으면 email 반환
     */
    private String getUserName(CompanyReview review) {
        if (review.getUser() == null) {
            return null;
        }

        return userProfileRepository.findByUserId(review.getUser().getId())
                .map(UserProfile::getName)
                .orElse(review.getUser().getEmail());
    }

    /**
     * 리뷰 작성
     */
    @Transactional
    public CompanyReviewResponse createReview(UUID companyUuid, String userEmail, CompanyReviewCreateRequest request) {
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
        updateCompanyRating(company.getId());

        log.info("리뷰 작성 완료: reviewUuid={}", review.getId());

        // 트랜잭션 내에서 Response 생성 (Lazy Loading 문제 방지)
        return toResponse(review);
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CompanyReviewResponse> getCompanyReviews(UUID companyUuid, Pageable pageable) {
        Company company = companyRepository.findByUuidAndIsDeletedFalse(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        Page<CompanyReview> reviews = reviewRepository.findByCompanyAndStatusOrderByCreatedAtDesc(company, "PUBLISHED", pageable);

        // 트랜잭션 내에서 Response 변환 (Lazy Loading 문제 방지)
        return reviews.map(this::toResponse);
    }

    /**
     * 리뷰 상세 조회
     */
    @Transactional(readOnly = true)
    public CompanyReviewResponse getReview(UUID reviewUuid) {
        CompanyReview review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN)); // REVIEW_NOT_FOUND 에러코드 추가 필요

        // 트랜잭션 내에서 Response 생성 (Lazy Loading 문제 방지)
        return toResponse(review);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public CompanyReviewResponse updateReview(UUID reviewUuid, String userEmail, CompanyReviewCreateRequest request) {
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

        // ✅ OneToMany 기반 이미지 업데이트 (비교 후 변경된 것만 처리)
        if (request.getImageUuids() != null) {
            updateReviewImages(review, request.getImageUuids());
        }

        // 업체 평균 평점 업데이트
        updateCompanyRating(review.getCompany().getId());

        log.info("리뷰 수정 완료: reviewUuid={}", reviewUuid);

        // 트랜잭션 내에서 Response 생성 (Lazy Loading 문제 방지)
        return toResponse(review);
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

        Long companyId = review.getCompany().getId();

        // 리뷰 이미지 soft delete 처리
        reviewImageRepository.softDeleteByReviewId(review.getId(), java.time.LocalDateTime.now());
        log.info("리뷰 이미지 soft delete 완료: reviewId={}", review.getId());

        review.softDelete();
        reviewRepository.save(review);

        // 업체 평균 평점 업데이트
        updateCompanyRating(companyId);

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
    private void updateCompanyRating(Long companyId) {
        // Company를 새로 조회하여 Lazy Loading 문제 방지
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

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
    public CompanyReviewResponse toResponse(CompanyReview review) {
        // ✅ OneToMany 기반 이미지 조회 사용 (UUID 포함)
        List<ReviewImageDto> imageDtos = getReviewImageDtos(review);
        String userName = getUserName(review);

        // Company를 새로 조회하여 Lazy Loading 문제 방지
        Long companyId = review.getCompany().getId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        return CompanyReviewResponse.from(review, imageDtos, userName, company.getId(), company.getName());
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
     * 리뷰 이미지 업데이트 (기존 이미지와 비교하여 변경된 것만 처리)
     * - 유지할 이미지: 그대로 둠
     * - 삭제할 이미지: soft delete
     * - 추가할 이미지: insert
     */
    private void updateReviewImages(CompanyReview review, String[] newImageUuids) {
        log.info("리뷰 이미지 업데이트 시작: reviewId={}", review.getId());

        // 1. 기존 이미지 목록 조회 (삭제되지 않은 것만)
        List<CompanyReviewImage> existingImages = reviewImageRepository
                .findByReviewAndIsDeletedFalseOrderByDisplayOrder(review);

        // 기존 이미지의 File ID Set
        java.util.Set<Long> existingFileIds = existingImages.stream()
                .map(CompanyReviewImage::getFileId)
                .collect(Collectors.toSet());

        // 2. 새 이미지 UUID를 File ID로 변환
        java.util.Set<Long> newFileIds = new java.util.HashSet<>();
        java.util.Map<Long, String> fileIdToUuidMap = new java.util.HashMap<>();

        if (newImageUuids != null && newImageUuids.length > 0) {
            for (String uuidStr : newImageUuids) {
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    File file = fileRepository.findByUuidAndIsDeletedFalse(uuid)
                            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
                    newFileIds.add(file.getId());
                    fileIdToUuidMap.put(file.getId(), uuidStr);
                }
            }
        }

        // 3. 삭제할 이미지 처리 (기존에 있지만 새 목록에 없는 것)
        int deletedCount = 0;
        for (CompanyReviewImage existingImage : existingImages) {
            if (!newFileIds.contains(existingImage.getFileId())) {
                existingImage.softDelete();
                reviewImageRepository.save(existingImage);
                deletedCount++;
            }
        }
        log.info("삭제된 이미지 수: {}", deletedCount);

        // 4. 추가할 이미지 처리 (새 목록에 있지만 기존에 없는 것)
        int addedCount = 0;
        int displayOrder = existingImages.size(); // 기존 이미지 뒤에 추가

        // 새 이미지 순서대로 처리하기 위해 배열 순서 유지
        if (newImageUuids != null) {
            for (String uuidStr : newImageUuids) {
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    File file = fileRepository.findByUuidAndIsDeletedFalse(uuid)
                            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

                    // 기존에 없는 파일만 추가
                    if (!existingFileIds.contains(file.getId())) {
                        // files 테이블의 entity 정보 업데이트
                        file.updateEntityInfo("REVIEW_IMAGE", review.getId());
                        fileRepository.save(file);

                        // company_review_images 테이블에 저장
                        CompanyReviewImage reviewImage = CompanyReviewImage.builder()
                                .review(review)
                                .fileId(file.getId())
                                .displayOrder(displayOrder++)
                                .build();
                        reviewImageRepository.save(reviewImage);
                        addedCount++;
                    }
                }
            }
        }
        log.info("추가된 이미지 수: {}", addedCount);
        log.info("리뷰 이미지 업데이트 완료: reviewId={}, 삭제={}, 추가={}", review.getId(), deletedCount, addedCount);
    }

    /**
     * 리뷰 이미지 조회 (File ID → URL 변환, 삭제되지 않은 것만)
     */
    public List<String> getReviewImageUrls(CompanyReview review) {
        List<CompanyReviewImage> images = reviewImageRepository
                .findByReviewAndIsDeletedFalseOrderByDisplayOrder(review);

        return images.stream()
                .map(image -> fileRepository.findById(image.getFileId())
                        .map(File::getFileUrl)
                        .orElse(null))
                .filter(url -> url != null)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 이미지 조회 (File ID → DTO 변환, UUID 포함, 삭제되지 않은 것만)
     */
    public List<ReviewImageDto> getReviewImageDtos(CompanyReview review) {
        List<CompanyReviewImage> images = reviewImageRepository
                .findByReviewAndIsDeletedFalseOrderByDisplayOrder(review);

        return images.stream()
                .map(image -> {
                    File file = fileRepository.findById(image.getFileId()).orElse(null);
                    if (file == null) {
                        return null;
                    }
                    return ReviewImageDto.from(
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
