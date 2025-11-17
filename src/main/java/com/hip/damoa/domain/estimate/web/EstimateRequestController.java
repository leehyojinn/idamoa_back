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
    @Operation(summary = "견적 요청 작성",
            description = "새로운 견적 요청을 작성합니다.\n\n" +
                    "**견적 상태 (status)**\n" +
                    "- 생성 시 기본 상태: PUBLISHED (발행됨)\n" +
                    "- 생성 즉시 공개 목록에 노출되어 업체들이 제안 가능\n" +
                    "- DRAFT 상태는 향후 임시저장 기능을 위해 예약됨\n\n" +
                    "**필수 항목**\n" +
                    "- title: 견적 요청 제목\n" +
                    "- description: 상세 설명\n" +
                    "- estimateType: 견적 유형 (인테리어/건축/리모델링 등)\n\n" +
                    "**선택 항목**\n" +
                    "- clientName: 사업장명 또는 고객명\n" +
                    "- businessType: 업종 (치과/카페/사무실 등)\n" +
                    "- areaPyeong: 평수\n" +
                    "- budgetMin/budgetMax: 예산 범위\n" +
                    "- desiredStartDate/desiredCompletionDate: 희망 일정\n" +
                    "- contactName/contactPhone: 연락처 정보\n" +
                    "- attachments: 첨부파일 (도면, 사진 등)")
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
    @Operation(summary = "견적 요청 수정",
            description = "내 견적 요청을 수정합니다.\n\n" +
                    "**중요**\n" +
                    "- DRAFT 상태인 견적만 수정 가능합니다\n" +
                    "- 발행(PUBLISHED) 후에는 수정이 불가능합니다\n" +
                    "- 본인이 작성한 견적만 수정 가능합니다\n\n" +
                    "**수정 가능한 필드**\n" +
                    "- 모든 필드 수정 가능 (null인 필드는 변경되지 않음)")
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
    @Operation(summary = "견적 요청 발행",
            description = "견적 요청을 발행하여 업체들이 볼 수 있게 합니다.\n\n" +
                    "**상태 변화**\n" +
                    "- DRAFT → PUBLISHED\n" +
                    "- 발행 후에는 수정이 불가능합니다\n" +
                    "- 업체들이 이 견적에 제안을 제출할 수 있게 됩니다\n\n" +
                    "**주의사항**\n" +
                    "- DRAFT 상태인 견적만 발행 가능\n" +
                    "- 본인이 작성한 견적만 발행 가능")
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
     * 견적 요청 상세 조회 (제안 목록 포함)
     */
    @Operation(summary = "견적 요청 상세 조회",
            description = "견적 요청 상세 정보를 조회합니다 (제안 목록 포함).\n\n" +
                    "**조회 가능 대상**\n" +
                    "- 공개(isPublic=true) 견적: 누구나 조회 가능 (로그인 불필요)\n" +
                    "- 비공개 견적: 작성자만 조회 가능 (로그인 필요)\n\n" +
                    "**제안 목록 권한별 필터링**\n" +
                    "- 요청자 본인: 모든 제안의 전체 내용 조회 (가격, 상세 설명 포함)\n" +
                    "- 제안 제출 업체: 본인이 제출한 제안만 전체 내용 조회\n" +
                    "- 기타 사용자: 제안 요약만 조회 (업체명, 제목, 날짜만 표시, 가격/상세 내용 숨김)\n\n" +
                    "**응답 구조**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"request\": { /* 견적 요청 정보 */ },\n" +
                    "  \"proposals\": {\n" +
                    "    \"totalCount\": 5,\n" +
                    "    \"viewedCount\": 2,\n" +
                    "    \"accessLevel\": \"OWNER|PROPOSER|PUBLIC\",\n" +
                    "    \"items\": [ /* 권한에 따라 다른 레벨의 제안 정보 */ ]\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n\n" +
                    "**부가 기능**\n" +
                    "- 조회 시 viewCount 자동 증가\n" +
                    "- 첨부파일 정보 포함")
    @GetMapping("/{requestUuid}")
    public ApiResponse<EstimateRequestDetailResponse> getEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid) {

        // 로그인 안 한 경우 null, 로그인한 경우 email
        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        EstimateRequestDetailResponse response = estimateRequestService
                .getEstimateRequestDetailByUuid(userEmail, requestUuid);

        return ApiResponse.success(response);
    }

    /**
     * 내 견적 요청 목록 조회
     */
    @Operation(summary = "내 견적 요청 목록",
            description = "내가 작성한 견적 요청 목록을 조회합니다.\n\n" +
                    "**견적 상태 (status)**\n" +
                    "- PUBLISHED: 공개됨 (업체 제안 가능) - 생성 시 기본 상태\n" +
                    "- IN_PROGRESS: 진행중 (제안 검토 중)\n" +
                    "- MATCHED: 매칭됨 (업체 선정 완료)\n" +
                    "- COMPLETED: 완료 (프로젝트 완료)\n" +
                    "- CANCELLED: 취소됨\n" +
                    "- DRAFT: 작성중 (임시 저장, 향후 기능)\n\n" +
                    "**정렬**\n" +
                    "- 최신순 정렬 (createdAt DESC)")
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
    @Operation(summary = "공개 견적 요청 목록",
            description = "공개된 견적 요청 목록을 조회합니다.\n\n" +
                    "**조회 대상**\n" +
                    "- isPublic=true인 견적만 조회\n" +
                    "- PUBLISHED 상태 이상의 견적\n" +
                    "- 인증 없이 누구나 조회 가능\n\n" +
                    "**정렬**\n" +
                    "- 최신순 정렬 (createdAt DESC)\n\n" +
                    "**활용 예시**\n" +
                    "- 업체들이 제안할 견적 찾기\n" +
                    "- 메인 페이지 견적 목록 표시")
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
    @Operation(summary = "견적 요청 삭제",
            description = "내 견적 요청을 삭제합니다.\n\n" +
                    "**삭제 방식**\n" +
                    "- Soft Delete: 실제로 삭제되지 않고 isDeleted=true로 표시\n" +
                    "- 삭제된 데이터는 목록 조회 시 제외됨\n\n" +
                    "**주의사항**\n" +
                    "- 본인이 작성한 견적만 삭제 가능\n" +
                    "- 삭제 후 복구 불가")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{requestUuid}")
    public ApiResponse<Void> deleteEstimateRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid) {

        estimateRequestService.deleteEstimateRequestByUuid(userDetails.getUsername(), requestUuid);

        return ApiResponse.success();
    }
}