package com.hip.damoa.domain.planner.web;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 플래너 신청서 API (사용자용)
 */
@Slf4j
@Tag(name = "1013. Planner Application", description = "플래너 신청서 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/planner-applications")
public class PlannerApplicationController {

    private final PlannerApplicationService plannerApplicationService;

    // ===== 공개 API (인증 불필요) =====

    /**
     * 플래너 신청 목록 조회 (공개)
     */
    @Operation(summary = "플래너 신청 목록 조회",
            description = "플래너 신청 목록을 조회합니다. (비회원 접근 가능)\n\n" +
                    "**민감정보 제외**: 전화번호, 이메일은 응답에 포함되지 않습니다.\n\n" +
                    "**필터링**\n" +
                    "- status 파라미터로 특정 상태만 필터링 가능\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: status, updatedAt")
    @GetMapping("/list")
    public ApiResponse<Page<PlannerApplicationSummaryResponse>> getPlannerApplications(
            @RequestParam(required = false) PlannerApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PlannerApplicationSummaryResponse> response = plannerApplicationService
                .getPlannerApplications(status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청 상세 조회 (공개)
     */
    @Operation(summary = "플래너 신청 상세 조회",
            description = "플래너 신청 상세 정보를 조회합니다. (비회원 접근 가능)\n\n" +
                    "**민감정보 제외**: 전화번호, 이메일은 응답에 포함되지 않습니다.")
    @GetMapping("/detail/{applicationUuid}")
    public ApiResponse<PlannerApplicationDetailResponse> getPlannerApplication(
            @PathVariable UUID applicationUuid) {

        PlannerApplicationDetailResponse response = plannerApplicationService
                .getPlannerApplication(applicationUuid);

        return ApiResponse.success(response);
    }

    // ===== 인증 필요 API =====

    /**
     * 플래너 신청서 생성
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "플래너 신청서 생성",
            description = "USER가 플래너 상담 신청서를 생성합니다.\n\n" +
                    "**상담 방법 (consultationMethod)**\n" +
                    "- VISIT: 방문 상담\n" +
                    "- PHONE: 전화 상담\n" +
                    "- SNS: SNS 상담 (카카오톡 등)\n\n" +
                    "**요청 내용 (requestTypes, 다중 선택 가능)**\n" +
                    "- FULL_CONSULTING: 종합 컨설팅 (인테리어부터 운영까지)\n" +
                    "- NEW_OPENING: 신규 창업 컨설팅\n" +
                    "- REMODELING: 리모델링\n" +
                    "- OPERATION_CONSULTING: 운영 컨설팅\n" +
                    "- LEGAL_INQUIRY: 법률 자문\n\n" +
                    "**첨부파일**\n" +
                    "- attachmentFileUuids에 파일 UUID 배열로 전달\n" +
                    "- 전체 크기는 100MB 이하\n\n" +
                    "**희망 일정**\n" +
                    "- 1~3개까지 입력 가능 (우선순위 1, 2, 3)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<PlannerApplicationResponse> createApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlannerApplicationCreateRequest request) {

        PlannerApplicationResponse response = plannerApplicationService
                .createApplication(userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    /**
     * 내 플래너 신청서 목록 조회
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "내 플래너 신청서 목록 조회",
            description = "현재 사용자의 플래너 신청서 목록을 조회합니다.\n\n" +
                    "**신청서 상태 (status, 필터링 가능)**\n" +
                    "- PENDING: 대기중 (신청 접수 대기)\n" +
                    "- IN_PROGRESS: 진행중 (상담 진행 중)\n" +
                    "- COMPLETED: 완료 (상담 완료)\n" +
                    "- REJECTED: 거절 (신청 거절)\n\n" +
                    "- status 파라미터로 특정 상태만 필터링 가능\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: status, updatedAt")
    @GetMapping("/my")
    public ApiResponse<Page<PlannerApplicationListResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PlannerApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PlannerApplicationListResponse> response = plannerApplicationService
                .getMyApplications(userDetails.getUsername(), status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 상세 조회 (본인)
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "내 플래너 신청서 상세 조회",
            description = "본인의 플래너 신청서 상세 정보를 조회합니다.\n\n" +
                    "- 본인의 신청서만 조회 가능\n" +
                    "- UUID 기반 조회\n" +
                    "- 모든 정보 포함 (전화번호, 이메일 등)")
    @GetMapping("/my/{applicationUuid}")
    public ApiResponse<PlannerApplicationResponse> getApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid) {

        PlannerApplicationResponse response = plannerApplicationService
                .getApplication(userDetails.getUsername(), applicationUuid);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 수정
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "플래너 신청서 수정",
            description = "플래너 신청서를 수정합니다.\n\n" +
                    "**제한사항**\n" +
                    "- 본인의 신청서만 수정 가능\n" +
                    "- PENDING(대기중) 상태에서만 수정 가능\n\n" +
                    "**첨부파일 수정**\n" +
                    "- attachmentFileUuids에 수정 후 파일 UUID 배열 전달\n" +
                    "- 기존 파일 유지/삭제/추가 가능")
    @PutMapping("/{applicationUuid}")
    public ApiResponse<PlannerApplicationResponse> updateApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody PlannerApplicationUpdateRequest request) {

        PlannerApplicationResponse response = plannerApplicationService
                .updateApplication(userDetails.getUsername(), applicationUuid, request);

        return ApiResponse.success(response);
    }

    /**
     * 플래너 신청서 삭제
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "플래너 신청서 삭제",
            description = "플래너 신청서를 삭제합니다.\n\n" +
                    "**제한사항**\n" +
                    "- 본인의 신청서만 삭제 가능\n" +
                    "- PENDING(대기중) 상태에서만 삭제 가능\n" +
                    "- Soft Delete 처리됨")
    @DeleteMapping("/{applicationUuid}")
    public ApiResponse<Void> deleteApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID applicationUuid) {

        plannerApplicationService.deleteApplication(userDetails.getUsername(), applicationUuid);

        return ApiResponse.success();
    }
}
