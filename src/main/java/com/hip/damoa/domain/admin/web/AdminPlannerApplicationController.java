package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.service.PlannerApplicationService;
import com.hip.damoa.domain.planner.web.dto.*;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 플래너 신청서 관리자 API
 */
@Tag(name = "9940. Admin - Planner Application", description = "플래너 신청서 관리자 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/planner-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlannerApplicationController {

    private final PlannerApplicationService plannerApplicationService;

    /**
     * 전체 플래너 신청서 목록 조회 (관리자 전용)
     */
    @Operation(summary = "[관리자] 전체 플래너 신청서 목록 조회",
            description = "모든 플래너 신청서를 조회합니다.\n\n" +
                    "- 상태별 필터링 가능\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping
    public ApiResponse<Page<PlannerApplicationListResponse>> getAllApplications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PlannerApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("[관리자] 전체 플래너 신청서 목록 조회: adminEmail={}, status={}",
                userDetails.getUsername(), status);

        Page<PlannerApplicationListResponse> response = plannerApplicationService
                .getAllApplications(status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 상세 조회 (관리자 전용)
     */
    @Operation(summary = "[관리자] 플래너 신청서 상세 조회",
            description = "플래너 신청서의 상세 정보를 조회합니다.")
    @GetMapping("/{applicationUuid}")
    public ApiResponse<PlannerApplicationResponse> getApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid) {

        log.info("[관리자] 플래너 신청서 조회: adminEmail={}, uuid={}",
                userDetails.getUsername(), applicationUuid);

        PlannerApplicationResponse response = plannerApplicationService
                .getApplicationAdmin(applicationUuid);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 상태 변경
     */
    @Operation(summary = "[관리자] 플래너 신청서 상태 변경",
            description = "플래너 신청서의 상태를 변경합니다.\n\n" +
                    "- PENDING: 대기중\n" +
                    "- IN_PROGRESS: 진행중\n" +
                    "- COMPLETED: 완료\n" +
                    "- REJECTED: 거절")
    @PatchMapping("/{applicationUuid}/status")
    public ApiResponse<PlannerApplicationResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody PlannerApplicationStatusUpdateRequest request) {

        log.info("[관리자] 플래너 신청서 상태 변경: adminEmail={}, uuid={}, newStatus={}",
                userDetails.getUsername(), applicationUuid, request.getStatus());

        PlannerApplicationResponse response = plannerApplicationService
                .updateStatus(applicationUuid, request);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 답변 등록
     */
    @Operation(summary = "[관리자] 플래너 신청서 답변 등록",
            description = "플래너 신청서에 답변을 등록합니다.")
    @PatchMapping("/{applicationUuid}/response")
    public ApiResponse<PlannerApplicationResponse> addResponse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody PlannerApplicationResponseRequest request) {

        log.info("[관리자] 플래너 신청서 답변 등록: adminEmail={}, uuid={}",
                userDetails.getUsername(), applicationUuid);

        PlannerApplicationResponse response = plannerApplicationService
                .addResponse(applicationUuid, request);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 메모 등록
     */
    @Operation(summary = "[관리자] 플래너 신청서 메모 등록",
            description = "플래너 신청서에 관리자 메모를 등록합니다.")
    @PatchMapping("/{applicationUuid}/memo")
    public ApiResponse<PlannerApplicationResponse> addMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody PlannerApplicationMemoRequest request) {

        log.info("[관리자] 플래너 신청서 메모 등록: adminEmail={}, uuid={}",
                userDetails.getUsername(), applicationUuid);

        PlannerApplicationResponse response = plannerApplicationService
                .addMemo(applicationUuid, request);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 담당자 배정
     */
    @Operation(summary = "[관리자] 플래너 신청서 담당자 배정",
            description = "플래너 신청서에 담당 관리자를 배정합니다.")
    @PatchMapping("/{applicationUuid}/assign")
    public ApiResponse<PlannerApplicationResponse> assignAdmin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody PlannerApplicationAssignRequest request) {

        log.info("[관리자] 플래너 신청서 담당자 배정: adminEmail={}, uuid={}, adminId={}",
                userDetails.getUsername(), applicationUuid, request.getAdminId());

        PlannerApplicationResponse response = plannerApplicationService
                .assignAdmin(applicationUuid, request);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 삭제 (관리자)
     */
    @Operation(summary = "[관리자] 플래너 신청서 삭제",
            description = "플래너 신청서를 삭제합니다.\n\n" +
                    "- 모든 상태에서 삭제 가능\n" +
                    "- Soft Delete 처리됨")
    @DeleteMapping("/{applicationUuid}")
    public ApiResponse<Void> deleteApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid) {

        log.info("[관리자] 플래너 신청서 삭제: adminEmail={}, uuid={}",
                userDetails.getUsername(), applicationUuid);

        plannerApplicationService.deleteApplicationAdmin(userDetails.getUsername(), applicationUuid);

        return ApiResponse.success();
    }
}
