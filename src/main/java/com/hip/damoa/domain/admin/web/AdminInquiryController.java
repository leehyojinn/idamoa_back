package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.service.InquiryService;
import com.hip.damoa.domain.inquiry.web.dto.InquiryListResponse;
import com.hip.damoa.domain.inquiry.web.dto.InquiryResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 제휴/광고 문의 관리 API (관리자용)
 */
@Slf4j
@Tag(name = "1908. Admin - Inquiry", description = "제휴/광고 문의 관리 API (관리자)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;
    private final UserRepository userRepository;

    /**
     * 전체 문의 목록 조회
     */
    @Operation(summary = "전체 문의 목록 조회 (관리자)", description = "모든 문의를 조회합니다")
    @GetMapping
    public ApiResponse<Page<InquiryListResponse>> getAllInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Page<InquiryListResponse> response;
        if (status != null) {
            try {
                InquiryStatus statusEnum = InquiryStatus.valueOf(status);
                response = inquiryService.getInquiriesByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else {
            response = inquiryService.getInquiries(pageable);
        }

        return ApiResponse.success(response);
    }

    /**
     * 문의 상세 조회
     */
    @Operation(summary = "문의 상세 조회 (관리자)", description = "문의 상세 정보를 조회합니다")
    @GetMapping("/{inquiryUuid}")
    public ApiResponse<InquiryResponse> getInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        InquiryResponse response = inquiryService.getInquiry(inquiryUuid);
        return ApiResponse.success(response);
    }

    /**
     * 문의 상태 변경
     */
    @Operation(summary = "문의 상태 변경 (관리자)",
               description = "문의 처리 상태를 변경합니다\n\n" +
                       "가능한 상태값:\n" +
                       "- PENDING: 접수 대기\n" +
                       "- IN_PROGRESS: 처리 중\n" +
                       "- COMPLETED: 처리 완료\n" +
                       "- CANCELLED: 취소됨")
    @PatchMapping("/{inquiryUuid}/status")
    @Transactional
    public ApiResponse<InquiryResponse> changeStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Parameter(description = "문의 상태 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)",
                      required = true,
                      example = "IN_PROGRESS")
            @RequestParam String status) {

        log.info("관리자 문의 상태 변경: adminEmail={}, inquiryUuid={}, status={}",
                userDetails.getUsername(), inquiryUuid, status);

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        InquiryResponse response = inquiryService.changeStatus(inquiryUuid, status);

        log.info("관리자 문의 상태 변경 완료: inquiryUuid={}, status={}", inquiryUuid, status);

        return ApiResponse.success(response);
    }

    /**
     * 문의 삭제
     */
    @Operation(summary = "문의 삭제 (관리자)", description = "문의를 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{inquiryUuid}")
    @Transactional
    public ApiResponse<Void> deleteInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {

        log.info("관리자 문의 삭제: adminEmail={}, inquiryUuid={}",
                userDetails.getUsername(), inquiryUuid);

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        inquiryService.deleteInquiry(inquiryUuid);

        log.info("관리자 문의 삭제 완료: inquiryUuid={}", inquiryUuid);

        return ApiResponse.success();
    }
}
