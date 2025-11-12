package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.repository.EstimateRequestRepository;
import com.hip.damoa.domain.estimate.service.EstimateRequestService;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestListResponse;
import com.hip.damoa.domain.admin.web.dto.AdminEstimateRequestResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 견적 요청 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "15. Admin - EstimateRequest", description = "견적 요청 관리 API (관리자)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/estimate-requests")
public class AdminEstimateRequestController {

    private final EstimateRequestService estimateRequestService;
    private final EstimateRequestRepository estimateRequestRepository;
    private final UserRepository userRepository;

    /**
     * 관리자 - 모든 견적 요청 조회 (상태 무관)
     */
    @Operation(summary = "모든 견적 요청 조회 (관리자)", description = "모든 견적 요청을 조회합니다 (삭제 포함)")
    @GetMapping
    public ApiResponse<Page<EstimateRequestListResponse>> getAllEstimateRequests(
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

        Page<EstimateRequest> requests;
        if (status != null) {
            try {
                EstimateRequest.EstimateStatus statusEnum = EstimateRequest.EstimateStatus.valueOf(status);
                requests = estimateRequestRepository.findByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
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
    @Operation(summary = "견적 요청 상세 조회 (관리자)", description = "견적 요청 상세 정보를 조회합니다 (조회수 증가 없음)")
    @GetMapping("/{requestId}")
    public ApiResponse<AdminEstimateRequestResponse> getEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        EstimateRequest estimateRequest = estimateRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        return ApiResponse.success(AdminEstimateRequestResponse.from(estimateRequest));
    }

    /**
     * 관리자 - 견적 요청 삭제
     */
    @Operation(summary = "견적 요청 삭제 (관리자)", description = "모든 견적 요청을 삭제할 수 있습니다 (Soft Delete)")
    @DeleteMapping("/{requestId}")
    @Transactional
    public ApiResponse<Void> deleteEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        log.info("관리자 견적 요청 삭제: adminEmail={}, requestId={}", userDetails.getUsername(), requestId);

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        EstimateRequest estimateRequest = estimateRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESTIMATE_REQUEST_NOT_FOUND));

        estimateRequest.softDelete();
        estimateRequestRepository.save(estimateRequest);

        log.info("관리자 견적 요청 삭제 완료: id={}", requestId);

        return ApiResponse.success();
    }

    /**
     * 관리자 - 견적 요청 상태 변경
     */
    @Operation(summary = "견적 요청 상태 변경 (관리자)", description = "견적 요청 상태를 변경합니다")
    @PatchMapping("/{requestId}/status")
    @Transactional
    public ApiResponse<AdminEstimateRequestResponse> changeStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestParam String status) {

        log.info("관리자 견적 요청 상태 변경: adminEmail={}, requestId={}, status={}",
                userDetails.getUsername(), requestId, status);

        // 권한 확인
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        EstimateRequest estimateRequest = estimateRequestRepository.findById(requestId)
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
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        estimateRequestRepository.save(estimateRequest);

        log.info("관리자 견적 요청 상태 변경 완료: id={}, status={}", requestId, status);

        return ApiResponse.success(AdminEstimateRequestResponse.from(estimateRequest));
    }
}
