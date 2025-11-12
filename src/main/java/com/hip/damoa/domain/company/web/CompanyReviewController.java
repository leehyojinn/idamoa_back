package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.service.CompanyReviewService;
import com.hip.damoa.domain.company.web.dto.CompanyReviewCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyReviewReplyRequest;
import com.hip.damoa.domain.company.web.dto.CompanyReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "07. Company Review", description = "업체 리뷰 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CompanyReviewController {

    private final CompanyReviewService reviewService;

    /**
     * 리뷰 작성
     */
    @Operation(summary = "리뷰 작성", description = "업체에 대한 리뷰를 작성합니다")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/companies/{companyUuid}/reviews")
    public ApiResponse<CompanyReviewResponse> createReview(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        log.info("리뷰 작성 API 호출: companyUuid={}, userEmail={}", companyUuid, userDetails.getUsername());

        CompanyReview review = reviewService.createReview(
                companyUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(reviewService.toResponse(review));
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Operation(summary = "업체 리뷰 목록 조회", description = "특정 업체의 리뷰 목록을 조회합니다 (페이징)")
    @GetMapping("/companies/{companyUuid}/reviews")
    public ApiResponse<Page<CompanyReviewResponse>> getCompanyReviews(
            @PathVariable UUID companyUuid,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("업체 리뷰 목록 조회 API 호출: companyUuid={}, page={}, size={}",
                companyUuid, pageable.getPageNumber(), pageable.getPageSize());

        Page<CompanyReview> reviews = reviewService.getCompanyReviews(companyUuid, pageable);
        Page<CompanyReviewResponse> response = reviews.map(reviewService::toResponse);

        return ApiResponse.success(response);
    }

    /**
     * 리뷰 상세 조회
     */
    @Operation(summary = "리뷰 상세 조회", description = "특정 리뷰의 상세 정보를 조회합니다")
    @GetMapping("/reviews/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> getReview(@PathVariable UUID reviewUuid) {

        log.info("리뷰 상세 조회 API 호출: reviewUuid={}", reviewUuid);

        CompanyReview review = reviewService.getReview(reviewUuid);

        return ApiResponse.success(reviewService.toResponse(review));
    }

    /**
     * 리뷰 수정
     */
    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다 (작성자만 가능)")
    @PutMapping("/reviews/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> updateReview(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        log.info("리뷰 수정 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        CompanyReview review = reviewService.updateReview(
                reviewUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(reviewService.toResponse(review));
    }

    /**
     * 리뷰 삭제
     */
    @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다 (작성자만 가능)")
    @DeleteMapping("/reviews/{reviewUuid}")
    public ApiResponse<Void> deleteReview(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("리뷰 삭제 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        reviewService.deleteReview(reviewUuid, userDetails.getUsername());

        return ApiResponse.success();
    }

    /**
     * 업체 답변 작성
     */
    @Operation(summary = "업체 답변 작성", description = "리뷰에 대한 업체 답변을 작성합니다 (업체 소유자만 가능)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<CompanyReviewResponse> addReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        log.info("업체 답변 작성 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        CompanyReview review = reviewService.addReply(
                reviewUuid,
                userDetails.getUsername(),
                request.getReply()
        );

        return ApiResponse.success(reviewService.toResponse(review));
    }

    /**
     * 업체 답변 수정
     */
    @Operation(summary = "업체 답변 수정", description = "작성한 업체 답변을 수정합니다 (업체 소유자만 가능)")
    @PutMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<CompanyReviewResponse> updateReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        log.info("업체 답변 수정 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        CompanyReview review = reviewService.updateReply(
                reviewUuid,
                userDetails.getUsername(),
                request.getReply()
        );

        return ApiResponse.success(reviewService.toResponse(review));
    }

    /**
     * 업체 답변 삭제
     */
    @Operation(summary = "업체 답변 삭제", description = "작성한 업체 답변을 삭제합니다 (업체 소유자만 가능)")
    @DeleteMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<Void> deleteReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("업체 답변 삭제 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        reviewService.deleteReply(reviewUuid, userDetails.getUsername());

        return ApiResponse.success();
    }
}
