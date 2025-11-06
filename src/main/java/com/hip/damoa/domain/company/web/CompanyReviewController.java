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

@Tag(name = "Company Review", description = "업체 리뷰 관련 API")
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
    @PostMapping("/companies/{companyId}/reviews")
    public ApiResponse<CompanyReviewResponse> createReview(
            @PathVariable Long companyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        log.info("리뷰 작성 API 호출: companyId={}, userEmail={}", companyId, userDetails.getUsername());

        CompanyReview review = reviewService.createReview(
                companyId,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(CompanyReviewResponse.from(review));
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Operation(summary = "업체 리뷰 목록 조회", description = "특정 업체의 리뷰 목록을 조회합니다 (페이징)")
    @GetMapping("/companies/{companyId}/reviews")
    public ApiResponse<Page<CompanyReviewResponse>> getCompanyReviews(
            @PathVariable Long companyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("업체 리뷰 목록 조회 API 호출: companyId={}, page={}, size={}",
                companyId, pageable.getPageNumber(), pageable.getPageSize());

        Page<CompanyReview> reviews = reviewService.getCompanyReviews(companyId, pageable);
        Page<CompanyReviewResponse> response = reviews.map(CompanyReviewResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 리뷰 상세 조회
     */
    @Operation(summary = "리뷰 상세 조회", description = "특정 리뷰의 상세 정보를 조회합니다")
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<CompanyReviewResponse> getReview(@PathVariable Long reviewId) {

        log.info("리뷰 상세 조회 API 호출: reviewId={}", reviewId);

        CompanyReview review = reviewService.getReview(reviewId);

        return ApiResponse.success(CompanyReviewResponse.from(review));
    }

    /**
     * 리뷰 수정
     */
    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다 (작성자만 가능)")
    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<CompanyReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        log.info("리뷰 수정 API 호출: reviewId={}, userEmail={}", reviewId, userDetails.getUsername());

        CompanyReview review = reviewService.updateReview(
                reviewId,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(CompanyReviewResponse.from(review));
    }

    /**
     * 리뷰 삭제
     */
    @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다 (작성자만 가능)")
    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("리뷰 삭제 API 호출: reviewId={}, userEmail={}", reviewId, userDetails.getUsername());

        reviewService.deleteReview(reviewId, userDetails.getUsername());

        return ApiResponse.success();
    }

    /**
     * 업체 답변 작성
     */
    @Operation(summary = "업체 답변 작성", description = "리뷰에 대한 업체 답변을 작성합니다 (업체 소유자만 가능)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/reviews/{reviewId}/reply")
    public ApiResponse<CompanyReviewResponse> addReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        log.info("업체 답변 작성 API 호출: reviewId={}, userEmail={}", reviewId, userDetails.getUsername());

        CompanyReview review = reviewService.addReply(
                reviewId,
                userDetails.getUsername(),
                request.getReply()
        );

        return ApiResponse.success(CompanyReviewResponse.from(review));
    }

    /**
     * 업체 답변 수정
     */
    @Operation(summary = "업체 답변 수정", description = "작성한 업체 답변을 수정합니다 (업체 소유자만 가능)")
    @PutMapping("/reviews/{reviewId}/reply")
    public ApiResponse<CompanyReviewResponse> updateReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        log.info("업체 답변 수정 API 호출: reviewId={}, userEmail={}", reviewId, userDetails.getUsername());

        CompanyReview review = reviewService.updateReply(
                reviewId,
                userDetails.getUsername(),
                request.getReply()
        );

        return ApiResponse.success(CompanyReviewResponse.from(review));
    }

    /**
     * 업체 답변 삭제
     */
    @Operation(summary = "업체 답변 삭제", description = "작성한 업체 답변을 삭제합니다 (업체 소유자만 가능)")
    @DeleteMapping("/reviews/{reviewId}/reply")
    public ApiResponse<Void> deleteReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("업체 답변 삭제 API 호출: reviewId={}, userEmail={}", reviewId, userDetails.getUsername());

        reviewService.deleteReply(reviewId, userDetails.getUsername());

        return ApiResponse.success();
    }
}
