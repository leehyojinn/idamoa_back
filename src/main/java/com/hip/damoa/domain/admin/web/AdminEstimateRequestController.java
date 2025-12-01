package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.model.EstimateStatus;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.service.EstimateRequestService;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestListResponse;
import com.hip.damoa.domain.admin.web.dto.AdminEstimateRequestResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 견적 요청 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "1903. Admin - EstimateRequest", description = "견적 요청 관리 API (관리자)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/estimate-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEstimateRequestController {

    private final EstimateRequestService estimateRequestService;
    private final EstimateRequestRepository estimateRequestRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * EstimateRequest의 User로부터 userName 조회
     * UserProfile이 있으면 name 반환, 없으면 email 반환
     */
    private String getUserName(EstimateRequest request) {
        if (request.getUser() == null) {
            return null;
        }

        return userProfileRepository.findByUserId(request.getUser().getId())
                .map(com.hip.damoa.domain.user.model.UserProfile::getName)
                .orElse(request.getUser().getEmail());
    }

    /**
     * 관리자 - 모든 견적 요청 조회/검색 (상태 무관)
     */
    @Operation(summary = "모든 견적 요청 조회/검색 (관리자)",
            description = "모든 견적 요청을 조회합니다 (삭제 포함).\n\n" +
                    "**검색 필터 (모두 선택사항)**:\n" +
                    "- `keyword`: 제목, 내용 검색\n" +
                    "- `status`: 상태 필터 (DRAFT, PUBLISHED, CANCELLED, COMPLETED)")
    @GetMapping
    public ApiResponse<Page<EstimateRequestListResponse>> getAllEstimateRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제목/내용 검색") @RequestParam(required = false) String keyword,
            @Parameter(description = "상태 필터 (DRAFT, PUBLISHED, CANCELLED, COMPLETED)") @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 견적 요청 목록 조회: adminEmail={}, keyword={}, status={}",
                userDetails.getUsername(), keyword, status);

        Page<EstimateRequest> requests;
        if (status != null) {
            try {
                EstimateStatus statusEnum = EstimateStatus.valueOf(status);
                requests = estimateRequestRepository.findByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_ESTIMATE_STATUS);
            }
        } else {
            requests = estimateRequestRepository.findAll(pageable);
        }

        Page<EstimateRequestListResponse> response = requests.map(EstimateRequestListResponse::from);
        return ApiResponse.success(response);
    }

    /**
     * 관리자 - 견적 요청 상세 조회 (조회수 증가 없음)
     */
    @Operation(summary = "견적 요청 상세 조회 (관리자)", description = "견적 요청 상세 정보를 조회합니다 (조회수 증가 없음, 삭제된 데이터 포함)")
    @GetMapping("/{requestUuid}")
    public ApiResponse<AdminEstimateRequestResponse> getEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "견적 요청 UUID") @PathVariable UUID requestUuid) {
        log.info("[관리자] 견적 요청 상세 조회: adminEmail={}, requestUuid={}",
                userDetails.getUsername(), requestUuid);

        // 삭제된 데이터도 조회 가능
        EstimateRequest estimateRequest = estimateRequestRepository.findByUuid(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        String userName = getUserName(estimateRequest);
        return ApiResponse.success(AdminEstimateRequestResponse.from(estimateRequest, userName));
    }

    /**
     * 관리자 - 견적 요청 삭제
     */
    @Operation(summary = "견적 요청 삭제 (관리자)", description = "모든 견적 요청을 삭제할 수 있습니다 (Soft Delete)")
    @DeleteMapping("/{requestUuid}")
    @Transactional
    public ApiResponse<Void> deleteEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "견적 요청 UUID") @PathVariable UUID requestUuid) {
        log.info("[관리자] 견적 요청 삭제: adminEmail={}, requestUuid={}", userDetails.getUsername(), requestUuid);

        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        estimateRequest.softDelete();
        estimateRequestRepository.save(estimateRequest);

        return ApiResponse.success();
    }

    /**
     * 관리자 - 견적 요청 상태 변경
     */
    @Operation(summary = "견적 요청 상태 변경 (관리자)", description = "견적 요청 상태를 변경합니다")
    @PatchMapping("/{requestUuid}/status")
    @Transactional
    public ApiResponse<AdminEstimateRequestResponse> changeStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "견적 요청 UUID") @PathVariable UUID requestUuid,
            @Parameter(description = "변경할 상태 (PUBLISHED, CANCELLED, COMPLETED)") @RequestParam String status) {
        log.info("[관리자] 견적 요청 상태 변경: adminEmail={}, requestUuid={}, status={}",
                userDetails.getUsername(), requestUuid, status);

        EstimateRequest estimateRequest = estimateRequestRepository.findByUuidAndIsDeletedFalse(requestUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        // 상태에 따라 메서드 호출
        switch (status) {
            case "PUBLISHED":
                estimateRequest.publish();
                break;
            case "CANCELLED":
                estimateRequest.cancel();
                break;
            case "COMPLETED":
                estimateRequest.complete();
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_ESTIMATE_STATUS);
        }

        estimateRequestRepository.save(estimateRequest);

        String userName = getUserName(estimateRequest);
        return ApiResponse.success(AdminEstimateRequestResponse.from(estimateRequest, userName));
    }
}
