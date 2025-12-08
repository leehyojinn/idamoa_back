package com.hip.damoa.domain.inquiry.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.inquiry.service.InquiryService;
import com.hip.damoa.domain.inquiry.web.dto.*;
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

/**
 * 일반 문의 API (사용자용)
 */
@Slf4j
@Tag(name = "17. Inquiry", description = "일반 문의 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 작성
     */
    @Operation(summary = "문의 작성",
            description = "일반 문의를 작성합니다. 로그인이 필요합니다.\n\n" +
                    "문의 유형:\n" +
                    "- BUG: 버그 신고\n" +
                    "- PAYMENT_ERROR: 결제 오류\n" +
                    "- ACCOUNT_ISSUE: 계정 문제\n" +
                    "- SUGGESTION: 건의사항\n" +
                    "- OTHER: 기타")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<InquiryResponse> createInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InquiryCreateRequest request) {

        InquiryResponse response = inquiryService.createInquiry(userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * 내 문의 목록 조회
     */
    @Operation(summary = "내 문의 목록 조회",
            description = "로그인한 사용자의 문의 목록을 조회합니다.\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: updatedAt, status")
    @GetMapping("/my")
    public ApiResponse<Page<InquiryListResponse>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InquiryListResponse> responses = inquiryService.getMyInquiries(userDetails.getUsername(), pageable);
        return ApiResponse.success(responses);
    }

    /**
     * 내 문의 검색
     */
    @Operation(summary = "내 문의 검색",
            description = "로그인한 사용자의 문의를 검색합니다.\n\n" +
                    "검색 조건:\n" +
                    "- keyword: 제목/내용 검색어\n" +
                    "- inquiryType: 문의 유형\n" +
                    "- status: 문의 상태\n" +
                    "- startDate/endDate: 작성일 범위\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: updatedAt, status")
    @GetMapping("/my/search")
    public ApiResponse<Page<InquiryListResponse>> searchMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute InquirySearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InquiryListResponse> responses = inquiryService.searchMyInquiries(
                userDetails.getUsername(), request, pageable);
        return ApiResponse.success(responses);
    }

    /**
     * 내 문의 상세 조회
     */
    @Operation(summary = "내 문의 상세 조회",
            description = "로그인한 사용자의 특정 문의를 상세 조회합니다.")
    @GetMapping("/my/{inquiryUuid}")
    public ApiResponse<InquiryResponse> getMyInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {

        InquiryResponse response = inquiryService.getMyInquiry(userDetails.getUsername(), inquiryUuid);
        return ApiResponse.success(response);
    }

    /**
     * 내 문의 수정
     */
    @Operation(summary = "내 문의 수정",
            description = "로그인한 사용자의 문의를 수정합니다. PENDING 상태일 때만 가능합니다.")
    @PutMapping("/my/{inquiryUuid}")
    public ApiResponse<InquiryResponse> updateMyInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Valid @RequestBody InquiryUpdateRequest request) {

        InquiryResponse response = inquiryService.updateMyInquiry(userDetails.getUsername(), inquiryUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 내 문의 삭제
     */
    @Operation(summary = "내 문의 삭제",
            description = "로그인한 사용자의 문의를 삭제합니다. PENDING 상태일 때만 가능합니다.")
    @DeleteMapping("/my/{inquiryUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteMyInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {

        inquiryService.deleteMyInquiry(userDetails.getUsername(), inquiryUuid);
        return ApiResponse.success();
    }
}