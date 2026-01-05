package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.service.CompanyReviewService;
import com.hip.damoa.domain.company.web.dto.CompanyReviewCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 리뷰 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "1910. Admin - Review", description = "리뷰 관리 API (관리자)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final CompanyReviewService companyReviewService;

    /**
     * 전체 리뷰 목록 조회
     */
    @Operation(summary = "[관리자] 전체 리뷰 목록 조회",
            description = """
                    전체 리뷰 목록을 조회합니다.

                    ## 검색 필터
                    - keyword: 제목, 내용으로 검색 (선택)
                    - status: 상태별 필터 (PUBLISHED, HIDDEN, REPORTED) (선택)

                    ## 정렬
                    - 기본값: createdAt DESC (최신순)
                    - 사용법: sort=createdAt,desc 또는 sort=createdAt,asc
                    """)
    @GetMapping
    public ApiResponse<Page<CompanyReviewResponse>> getAllReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "검색 키워드 (제목, 내용)") @RequestParam(required = false) String keyword,
            @Parameter(description = "상태 필터 (PUBLISHED, HIDDEN, REPORTED)") @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 전체 리뷰 조회: adminEmail={}, keyword={}, status={}",
                userDetails.getUsername(), keyword, status);
        Page<CompanyReviewResponse> response = companyReviewService.getAllReviewsForAdmin(keyword, status, pageable);
        return ApiResponse.success(response);
    }

    /**
     * 리뷰 상세 조회
     */
    @Operation(summary = "[관리자] 리뷰 상세 조회",
            description = "리뷰 상세 정보를 조회합니다. 모든 상태의 리뷰를 조회할 수 있습니다.")
    @GetMapping("/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> getReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "리뷰 UUID") @PathVariable UUID reviewUuid) {
        log.info("[관리자] 리뷰 상세 조회: adminEmail={}, reviewUuid={}",
                userDetails.getUsername(), reviewUuid);
        CompanyReviewResponse response = companyReviewService.getReviewForAdmin(reviewUuid);
        return ApiResponse.success(response);
    }

    /**
     * 리뷰 수정
     */
    @Operation(summary = "[관리자] 리뷰 수정",
            description = """
                    리뷰를 수정합니다. 권한 검증 없이 수정 가능합니다.

                    ## 수정 가능 항목
                    - rating: 평점 (1.0 ~ 5.0)
                    - title: 제목
                    - content: 내용
                    - imageUuids: 이미지 UUID 목록
                    """)
    @PutMapping("/{reviewUuid}")
    public ApiResponse<CompanyReviewResponse> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "리뷰 UUID") @PathVariable UUID reviewUuid,
            @Valid @RequestBody CompanyReviewCreateRequest request) {
        log.info("[관리자] 리뷰 수정: adminEmail={}, reviewUuid={}",
                userDetails.getUsername(), reviewUuid);
        CompanyReviewResponse response = companyReviewService.updateReviewByAdmin(reviewUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 리뷰 상태 변경
     */
    @Operation(summary = "[관리자] 리뷰 상태 변경",
            description = """
                    리뷰의 상태를 변경합니다.

                    ## 상태 종류
                    - PUBLISHED: 게시됨 (공개)
                    - HIDDEN: 숨김 (비공개)
                    - REPORTED: 신고됨
                    """)
    @PatchMapping("/{reviewUuid}/status")
    public ApiResponse<CompanyReviewResponse> updateReviewStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "리뷰 UUID") @PathVariable UUID reviewUuid,
            @Parameter(description = "변경할 상태") @RequestParam String status) {
        log.info("[관리자] 리뷰 상태 변경: adminEmail={}, reviewUuid={}, status={}",
                userDetails.getUsername(), reviewUuid, status);
        CompanyReviewResponse response = companyReviewService.updateReviewStatusByAdmin(reviewUuid, status);
        return ApiResponse.success(response);
    }

    /**
     * 업체 답변 수정/삭제
     */
    @Operation(summary = "[관리자] 업체 답변 수정/삭제",
            description = """
                    업체 답변을 수정하거나 삭제합니다.

                    - 답변 수정: reply 파라미터에 내용 전달
                    - 답변 삭제: reply 파라미터를 빈 문자열로 전달
                    """)
    @PatchMapping("/{reviewUuid}/reply")
    public ApiResponse<CompanyReviewResponse> updateReply(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "리뷰 UUID") @PathVariable UUID reviewUuid,
            @Parameter(description = "답변 내용 (빈 문자열로 전달 시 삭제)") @RequestParam(required = false) String reply) {
        log.info("[관리자] 업체 답변 수정: adminEmail={}, reviewUuid={}",
                userDetails.getUsername(), reviewUuid);
        CompanyReviewResponse response = companyReviewService.updateReplyByAdmin(reviewUuid, reply);
        return ApiResponse.success(response);
    }

    /**
     * 리뷰 삭제
     */
    @Operation(summary = "[관리자] 리뷰 삭제",
            description = "리뷰를 삭제합니다 (Soft Delete). 권한 검증 없이 삭제 가능합니다.")
    @DeleteMapping("/{reviewUuid}")
    public ApiResponse<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "리뷰 UUID") @PathVariable UUID reviewUuid) {
        log.info("[관리자] 리뷰 삭제: adminEmail={}, reviewUuid={}",
                userDetails.getUsername(), reviewUuid);
        companyReviewService.deleteReviewByAdmin(reviewUuid);
        return ApiResponse.success();
    }
}
