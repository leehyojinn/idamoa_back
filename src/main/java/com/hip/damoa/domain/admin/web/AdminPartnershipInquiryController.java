package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.partnership.service.PartnershipInquiryService;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryListResponse;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 제휴/광고 문의 관리자 컨트롤러
 */
@Slf4j
@Tag(name = "1912. Admin Partnership Inquiry", description = "제휴/광고 문의 관리자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/partnership-inquiries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnershipInquiryController {

    private final PartnershipInquiryService partnershipInquiryService;

    @Operation(summary = "제휴/광고 문의 목록 조회", description = "제휴/광고 문의 목록을 조회합니다. 상태와 유형으로 필터링할 수 있습니다.")
    @GetMapping
    public ApiResponse<Page<PartnershipInquiryListResponse>> getInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "처리 상태 필터\n\n" +
                    "- `PENDING`: 대기중\n" +
                    "- `IN_PROGRESS`: 처리중\n" +
                    "- `COMPLETED`: 완료\n" +
                    "- `CANCELLED`: 취소",
                    example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "문의 유형 필터\n\n" +
                    "- `PARTNERSHIP`: 제휴 문의\n" +
                    "- `ADVERTISEMENT`: 광고 문의\n" +
                    "- `OTHER`: 기타",
                    example = "PARTNERSHIP")
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 제휴/광고 문의 목록 조회: adminEmail={}, status={}, type={}",
                userDetails.getUsername(), status, type);
        Page<PartnershipInquiryListResponse> response = partnershipInquiryService.getInquiries(
                status, type, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "제휴/광고 문의 상세 조회", description = "제휴/광고 문의 상세 정보를 조회합니다")
    @GetMapping("/{inquiryUuid}")
    public ApiResponse<PartnershipInquiryResponse> getInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {
        log.info("[관리자] 제휴/광고 문의 상세 조회: adminEmail={}, inquiryUuid={}",
                userDetails.getUsername(), inquiryUuid);
        PartnershipInquiryResponse response = partnershipInquiryService.getInquiry(inquiryUuid);
        return ApiResponse.success(response);
    }

    @Operation(summary = "제휴/광고 문의 상태 변경", description = "제휴/광고 문의 상태를 변경합니다")
    @PatchMapping("/{inquiryUuid}/status")
    public ApiResponse<Void> changeStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid,
            @Parameter(description = "변경할 상태값\n\n" +
                    "- `PENDING`: 대기중\n" +
                    "- `IN_PROGRESS`: 처리중\n" +
                    "- `COMPLETED`: 완료\n" +
                    "- `CANCELLED`: 취소",
                    required = true,
                    example = "IN_PROGRESS")
            @RequestParam String status) {
        log.info("[관리자] 제휴/광고 문의 상태 변경: adminEmail={}, inquiryUuid={}, status={}",
                userDetails.getUsername(), inquiryUuid, status);
        partnershipInquiryService.changeStatus(inquiryUuid, status);
        return ApiResponse.success();
    }

    @Operation(summary = "제휴/광고 문의 삭제", description = "제휴/광고 문의를 삭제합니다 (소프트 삭제)")
    @DeleteMapping("/{inquiryUuid}")
    public ApiResponse<Void> deleteInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID inquiryUuid) {
        log.info("[관리자] 제휴/광고 문의 삭제: adminEmail={}, inquiryUuid={}",
                userDetails.getUsername(), inquiryUuid);
        partnershipInquiryService.deleteInquiry(inquiryUuid);
        return ApiResponse.success();
    }
}