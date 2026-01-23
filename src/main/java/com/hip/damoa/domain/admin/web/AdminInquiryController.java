package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.inquiry.service.InquiryService;
import com.hip.damoa.domain.inquiry.web.dto.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 일반 문의 관리 API (관리자용)
 */
@Slf4j
@Tag(name = "9908. Admin - Inquiry", description = "일반 문의 관리 API (관리자)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 목록 조회/검색 (관리자용) - 통합 API
     */
    @Operation(summary = "문의 목록 조회/검색 (관리자용)",
            description = "일반 문의 목록을 조회합니다. 파라미터 없이 호출하면 전체 목록을 조회합니다.\n\n" +
                    "**검색 조건 (모두 선택사항)**:\n" +
                    "- `keyword`: 제목/내용 검색어\n" +
                    "- `inquiryType`: 문의 유형\n" +
                    "  - `BUG`: 버그 신고\n" +
                    "  - `PAYMENT_ERROR`: 결제 오류\n" +
                    "  - `ACCOUNT_ISSUE`: 계정 문제\n" +
                    "  - `SUGGESTION`: 건의사항\n" +
                    "  - `OTHER`: 기타\n" +
                    "- `status`: 문의 상태\n" +
                    "  - `PENDING`: 대기중\n" +
                    "  - `IN_PROGRESS`: 처리중\n" +
                    "  - `ANSWERED`: 답변완료\n" +
                    "  - `CLOSED`: 종료\n" +
                    "- `userEmail`: 작성자 이메일\n" +
                    "- `hasAnswer`: 답변 여부 (true/false)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping
    public ApiResponse<Page<InquiryListResponse>> getInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제목/내용 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "문의 유형 (BUG, PAYMENT_ERROR, ACCOUNT_ISSUE, SUGGESTION, OTHER)")
            @RequestParam(required = false) String inquiryType,
            @Parameter(description = "문의 상태 (PENDING, IN_PROGRESS, ANSWERED, CLOSED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "작성자 이메일") @RequestParam(required = false) String userEmail,
            @Parameter(description = "답변 여부") @RequestParam(required = false) Boolean hasAnswer,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[관리자] 문의 목록 조회: adminEmail={}, keyword={}, inquiryType={}, status={}",
                userDetails.getUsername(), keyword, inquiryType, status);
        Page<InquiryListResponse> responses = inquiryService.searchInquiries(
                keyword, inquiryType, status, userEmail, hasAnswer, pageable);
        return ApiResponse.success(responses);
    }

    /**
     * 문의 상세 조회 (관리자용)
     */
    @Operation(summary = "문의 상세 조회 (관리자용)",
            description = "특정 문의를 상세 조회합니다.")
    @GetMapping("/{inquiryUuid}")
    public ApiResponse<InquiryResponse> getInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {
        log.info("[관리자] 문의 상세 조회: adminEmail={}, inquiryUuid={}", userDetails.getUsername(), inquiryUuid);
        InquiryResponse response = inquiryService.getInquiry(inquiryUuid);
        return ApiResponse.success(response);
    }

    /**
     * 문의 상태 변경 (관리자용)
     */
    @Operation(summary = "문의 상태 변경 (관리자용)",
            description = "문의 상태를 변경합니다.\n\n" +
                    "가능한 상태:\n" +
                    "- IN_PROGRESS: 처리중\n" +
                    "- ANSWERED: 답변완료\n" +
                    "- CLOSED: 종료")
    @PutMapping("/{inquiryUuid}/status")
    public ApiResponse<InquiryResponse> changeStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Parameter(description = "변경할 상태", example = "IN_PROGRESS")
            @RequestParam String status) {
        log.info("[관리자] 문의 상태 변경: adminEmail={}, inquiryUuid={}, status={}",
                userDetails.getUsername(), inquiryUuid, status);
        InquiryResponse response = inquiryService.changeStatus(inquiryUuid, status);
        return ApiResponse.success(response);
    }

    /**
     * 문의 답변 작성 (관리자용)
     */
    @Operation(summary = "문의 답변 작성 (관리자용)",
            description = "문의에 대한 답변을 작성합니다. 한 문의당 하나의 답변만 가능합니다.")
    @PostMapping("/{inquiryUuid}/answer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryAnswerResponse> createAnswer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Valid @RequestBody InquiryAnswerCreateRequest request) {
        log.info("[관리자] 문의 답변 작성: adminEmail={}, inquiryUuid={}", userDetails.getUsername(), inquiryUuid);
        InquiryAnswerResponse response = inquiryService.createAnswer(userDetails.getUsername(), inquiryUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 문의 답변 수정 (관리자용)
     */
    @Operation(summary = "문의 답변 수정 (관리자용)",
            description = "작성된 답변을 수정합니다.")
    @PutMapping("/{inquiryUuid}/answer")
    public ApiResponse<InquiryAnswerResponse> updateAnswer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Valid @RequestBody InquiryAnswerCreateRequest request) {
        log.info("[관리자] 문의 답변 수정: adminEmail={}, inquiryUuid={}", userDetails.getUsername(), inquiryUuid);
        InquiryAnswerResponse response = inquiryService.updateAnswer(userDetails.getUsername(), inquiryUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 문의 답변 삭제 (관리자용)
     */
    @Operation(summary = "문의 답변 삭제 (관리자용)",
            description = "작성된 답변을 삭제합니다.")
    @DeleteMapping("/{inquiryUuid}/answer")
    public ApiResponse<Void> deleteAnswer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {
        log.info("[관리자] 문의 답변 삭제: adminEmail={}, inquiryUuid={}", userDetails.getUsername(), inquiryUuid);
        inquiryService.deleteAnswer(inquiryUuid);
        return ApiResponse.success();
    }

    /**
     * 문의 삭제 (관리자용)
     */
    @Operation(summary = "문의 삭제 (관리자용)",
            description = "문의를 삭제합니다. (Soft Delete)")
    @DeleteMapping("/{inquiryUuid}")
    public ApiResponse<Void> deleteInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {
        log.info("[관리자] 문의 삭제: adminEmail={}, inquiryUuid={}", userDetails.getUsername(), inquiryUuid);
        inquiryService.deleteInquiry(inquiryUuid);
        return ApiResponse.success();
    }
}