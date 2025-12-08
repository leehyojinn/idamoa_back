package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.service.CompanyReviewService;
import com.hip.damoa.domain.company.web.dto.CompanyReviewCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyReviewReplyRequest;
import com.hip.damoa.domain.company.web.dto.CompanyReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @Operation(summary = "리뷰 작성",
            description = "업체에 대한 리뷰를 작성합니다.\n\n" +
                    "**필수 정보:**\n" +
                    "- rating: 평점 (1~5점, 1.0~5.0 범위)\n" +
                    "- content: 리뷰 내용 (최소 10자 이상 권장)\n\n" +
                    "**선택 정보:**\n" +
                    "- title: 리뷰 제목\n" +
                    "- imageUrls: 리뷰 이미지 파일 ID 배열\n" +
                    "- tags: 태그 배열 (예: `[\"친절\", \"전문적\", \"빠름\"]`)\n" +
                    "- pros: 장점 (선택)\n" +
                    "- cons: 단점 (선택)\n\n" +
                    "**제약사항:**\n" +
                    "- 한 사용자당 한 업체에 하나의 리뷰만 작성 가능\n" +
                    "- 이미 리뷰를 작성한 경우 409 Conflict 발생\n" +
                    "- 자신의 업체에는 리뷰 작성 불가\n\n" +
                    "**리뷰 작성 후:**\n" +
                    "- 업체의 평균 평점 자동 업데이트\n" +
                    "- 리뷰 개수 자동 증가")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/companies/{companyUuid}/reviews")
    public ApiResponse<CompanyReviewResponse> createReview(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("리뷰 작성 API 호출: companyUuid={}, userEmail={}", companyUuid, userDetails.getUsername());

        CompanyReviewResponse response = reviewService.createReview(
                companyUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(response);
    }

    /**
     * 업체 리뷰 목록 조회
     */
    @Operation(summary = "업체 리뷰 목록 조회",
            description = "특정 업체의 모든 리뷰를 조회합니다.\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: rating\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 리뷰 작성자 정보 (이름, 프로필)\n" +
                    "- 평점, 내용, 이미지\n" +
                    "- 업체 답변 (있는 경우)\n" +
                    "- 작성일, 수정일\n\n" +
                    "**활용:**\n" +
                    "- 업체 상세 페이지 리뷰 섹션\n" +
                    "- 리뷰 더보기 페이지")
    @GetMapping("/companies/{companyUuid}/reviews")
    public ApiResponse<Page<CompanyReviewResponse>> getCompanyReviews(
            @PathVariable UUID companyUuid,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("업체 리뷰 목록 조회 API 호출: companyUuid={}, page={}, size={}",
                companyUuid, pageable.getPageNumber(), pageable.getPageSize());

        Page<CompanyReviewResponse> response = reviewService.getCompanyReviews(companyUuid, pageable);

        return ApiResponse.success(response);
    }

    /**
     * 리뷰 상세 조회
     */
    @Operation(summary = "리뷰 상세 조회", description = "특정 리뷰의 상세 정보를 조회합니다")
    @GetMapping("/reviews/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> getReview(@PathVariable UUID reviewUuid) {

        log.info("리뷰 상세 조회 API 호출: reviewUuid={}", reviewUuid);

        CompanyReviewResponse response = reviewService.getReview(reviewUuid);

        return ApiResponse.success(response);
    }

    /**
     * 리뷰 수정
     */
    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/reviews/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> updateReview(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewCreateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("리뷰 수정 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        CompanyReviewResponse response = reviewService.updateReview(
                reviewUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(response);
    }

    /**
     * 리뷰 삭제
     */
    @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/reviews/{reviewUuid}")
    public ApiResponse<Void> deleteReview(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("리뷰 삭제 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        reviewService.deleteReview(reviewUuid, userDetails.getUsername());

        return ApiResponse.success();
    }

    /**
     * 업체 답변 작성
     */
    @Operation(summary = "업체 답변 작성",
            description = "리뷰에 대한 업체의 답변을 작성합니다.\n\n" +
                    "**권한:**\n" +
                    "- 해당 업체의 소유자만 작성 가능\n" +
                    "- 다른 업체의 리뷰에는 답변 불가 (403 Forbidden)\n\n" +
                    "**필수 정보:**\n" +
                    "- reply: 답변 내용 (최소 10자 이상 권장)\n\n" +
                    "**제약사항:**\n" +
                    "- 한 리뷰당 하나의 답변만 가능\n" +
                    "- 이미 답변이 있는 경우 409 Conflict 발생 (수정 API 사용)\n\n" +
                    "**답변 작성 후:**\n" +
                    "- 리뷰 응답률 자동 업데이트\n" +
                    "- 리뷰 작성자에게 알림 발송 (구현 시)\n\n" +
                    "**활용:**\n" +
                    "- 고객 피드백에 대한 공식 응답\n" +
                    "- 오해 해명 및 개선 의지 표명\n" +
                    "- 고객 만족도 향상")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<CompanyReviewResponse> addReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

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
    @Operation(summary = "업체 답변 수정",
            description = "작성한 업체 답변을 수정합니다.\n\n" +
                    "**권한:**\n" +
                    "- 답변을 작성한 업체 소유자만 수정 가능\n\n" +
                    "**필수 정보:**\n" +
                    "- reply: 수정할 답변 내용\n\n" +
                    "**주의사항:**\n" +
                    "- 답변이 없는 경우 404 Not Found (작성 API 사용)\n" +
                    "- 수정 이력이 기록됨 (updatedAt)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<CompanyReviewResponse> updateReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyReviewReplyRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

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
    @Operation(summary = "업체 답변 삭제",
            description = "작성한 업체 답변을 삭제합니다.\n\n" +
                    "**권한:**\n" +
                    "- 답변을 작성한 업체 소유자만 삭제 가능\n\n" +
                    "**삭제 방식:**\n" +
                    "- 답변 내용 완전 삭제 (Soft Delete 아님)\n" +
                    "- reply, replyAt 필드가 null로 변경\n\n" +
                    "**주의사항:**\n" +
                    "- 삭제 후 복구 불가\n" +
                    "- 삭제 후 다시 작성 가능 (작성 API 사용)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/reviews/{reviewUuid}/reply")
    public ApiResponse<Void> deleteReply(
            @PathVariable UUID reviewUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("업체 답변 삭제 API 호출: reviewUuid={}, userEmail={}", reviewUuid, userDetails.getUsername());

        reviewService.deleteReply(reviewUuid, userDetails.getUsername());

        return ApiResponse.success();
    }
}
