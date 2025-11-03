package com.hip.damoa.domain.estimate.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.service.EstimateRequestService;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestListResponse;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestResponse;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestUpdateRequest;
import com.hip.damoa.domain.estimate.web.dto.*;
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

/**
 * 견적 요청 REST API (사용자용)
 */
@Slf4j
@Tag(name = "EstimateRequest", description = "견적 요청 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/estimate-requests")
public class EstimateRequestController {

    private final EstimateRequestService estimateRequestService;

    /**
     * 견적 요청 작성
     */
    @Operation(summary = "견적 요청 작성", description = "새로운 견적 요청을 작성합니다 (DRAFT 상태)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EstimateRequestResponse> createEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EstimateRequestCreateRequest request) {

        EstimateRequest estimateRequest = estimateRequestService.createEstimateRequest(
                userDetails.getUsername(), request);

        return ApiResponse.success(EstimateRequestResponse.from(estimateRequest));
    }

    /**
     * 견적 요청 수정
     */
    @Operation(summary = "견적 요청 수정", description = "내 견적 요청을 수정합니다 (DRAFT 상태만)")
    @PutMapping("/{requestId}")
    public ApiResponse<EstimateRequestResponse> updateEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody EstimateRequestUpdateRequest request) {

        EstimateRequest estimateRequest = estimateRequestService.updateEstimateRequest(
                userDetails.getUsername(), requestId, request);

        return ApiResponse.success(EstimateRequestResponse.from(estimateRequest));
    }

    /**
     * 견적 요청 발행 (DRAFT → PUBLISHED)
     */
    @Operation(summary = "견적 요청 발행", description = "견적 요청을 발행하여 업체들이 볼 수 있게 합니다")
    @PostMapping("/{requestId}/publish")
    public ApiResponse<EstimateRequestResponse> publishEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        EstimateRequest estimateRequest = estimateRequestService.publishEstimateRequest(
                userDetails.getUsername(), requestId);

        return ApiResponse.success(EstimateRequestResponse.from(estimateRequest));
    }

    /**
     * 견적 요청 상세 조회
     */
    @Operation(summary = "견적 요청 상세 조회", description = "견적 요청 상세 정보를 조회합니다 (조회수 증가)")
    @GetMapping("/{requestId}")
    public ApiResponse<EstimateRequestResponse> getEstimateRequest(
            @PathVariable Long requestId) {

        EstimateRequest estimateRequest = estimateRequestService.getEstimateRequest(requestId);

        return ApiResponse.success(EstimateRequestResponse.from(estimateRequest));
    }

    /**
     * 내 견적 요청 목록 조회
     */
    @Operation(summary = "내 견적 요청 목록", description = "내가 작성한 견적 요청 목록을 조회합니다")
    @GetMapping("/my")
    public ApiResponse<Page<EstimateRequestListResponse>> getMyEstimateRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<EstimateRequest> requests = estimateRequestService.getUserEstimateRequests(
                userDetails.getUsername(), pageable);

        Page<EstimateRequestListResponse> response = requests.map(EstimateRequestListResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 공개 견적 요청 목록 조회
     */
    @Operation(summary = "공개 견적 요청 목록", description = "공개된 견적 요청 목록을 조회합니다")
    @GetMapping
    public ApiResponse<Page<EstimateRequestListResponse>> getPublicEstimateRequests(
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<EstimateRequest> requests = estimateRequestService.getPublicEstimateRequests(pageable);

        Page<EstimateRequestListResponse> response = requests.map(EstimateRequestListResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 견적 요청 삭제
     */
    @Operation(summary = "견적 요청 삭제", description = "내 견적 요청을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{requestId}")
    public ApiResponse<Void> deleteEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        estimateRequestService.deleteEstimateRequest(userDetails.getUsername(), requestId);

        return ApiResponse.success();
    }
}
