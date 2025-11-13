package com.hip.damoa.domain.estimate.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateRequest;
import com.hip.damoa.domain.estimate.service.EstimateRequestService;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestListResponse;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestResponse;
import com.hip.damoa.domain.estimate.web.dto.EstimateRequestUpdateRequest;
import com.hip.damoa.domain.estimate.web.dto.*;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 견적 요청 REST API (사용자용)
 */
@Slf4j
@Tag(name = "08. EstimateRequest", description = "견적 요청 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/estimates/requests")
public class EstimateRequestController {

    private final EstimateRequestService estimateRequestService;
    private final FileRepository fileRepository;

    /**
     * 견적 요청에 첨부파일 정보 추가 (V30: 조인 테이블 방식)
     */
    private EstimateRequestResponse enrichWithFiles(EstimateRequest estimateRequest) {
        EstimateRequestResponse response = EstimateRequestResponse.from(estimateRequest);
        // Service의 getAttachmentResponses 호출하여 첨부파일 목록 설정
        List<AttachmentResponse> attachments = estimateRequestService.getAttachmentResponses(estimateRequest);
        response.setAttachments(attachments);
        return response;
    }

    /**
     * 견적 요청 작성
     */
    @Operation(summary = "견적 요청 작성", description = "새로운 견적 요청을 작성합니다 (DRAFT 상태)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EstimateRequestResponse> createEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EstimateRequestCreateRequest request) {

        EstimateRequest estimateRequest = estimateRequestService.createEstimateRequest(
                userDetails.getUsername(), request);

        return ApiResponse.success(enrichWithFiles(estimateRequest));
    }

    /**
     * 견적 요청 수정
     */
    @Operation(summary = "견적 요청 수정", description = "내 견적 요청을 수정합니다 (DRAFT 상태만)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{requestUuid}")
    public ApiResponse<EstimateRequestResponse> updateEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid,
            @Valid @RequestBody EstimateRequestUpdateRequest request) {

        EstimateRequest estimateRequest = estimateRequestService.updateEstimateRequestByUuid(
                userDetails.getUsername(), requestUuid, request);

        return ApiResponse.success(enrichWithFiles(estimateRequest));
    }

    /**
     * 견적 요청 발행 (DRAFT → PUBLISHED)
     */
    @Operation(summary = "견적 요청 발행", description = "견적 요청을 발행하여 업체들이 볼 수 있게 합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{requestUuid}/publish")
    public ApiResponse<EstimateRequestResponse> publishEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid) {

        EstimateRequest estimateRequest = estimateRequestService.publishEstimateRequestByUuid(
                userDetails.getUsername(), requestUuid);

        return ApiResponse.success(enrichWithFiles(estimateRequest));
    }

    /**
     * 견적 요청 상세 조회
     */
    @Operation(summary = "견적 요청 상세 조회", description = "견적 요청 상세 정보를 조회합니다 (조회수 증가)")
    @GetMapping("/{requestUuid}")
    public ApiResponse<EstimateRequestResponse> getEstimateRequest(
            @PathVariable UUID requestUuid) {

        EstimateRequest estimateRequest = estimateRequestService.getEstimateRequestByUuid(requestUuid);

        return ApiResponse.success(enrichWithFiles(estimateRequest));
    }

    /**
     * 내 견적 요청 목록 조회
     */
    @Operation(summary = "내 견적 요청 목록", description = "내가 작성한 견적 요청 목록을 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
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
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<EstimateRequest> requests = estimateRequestService.getPublicEstimateRequests(pageable);

        Page<EstimateRequestListResponse> response = requests.map(EstimateRequestListResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 견적 요청 삭제
     */
    @Operation(summary = "견적 요청 삭제", description = "내 견적 요청을 삭제합니다 (Soft Delete)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{requestUuid}")
    public ApiResponse<Void> deleteEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid) {

        estimateRequestService.deleteEstimateRequestByUuid(userDetails.getUsername(), requestUuid);

        return ApiResponse.success();
    }
}