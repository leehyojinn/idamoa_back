package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.admin.service.AdminUserService;
import com.hip.damoa.domain.admin.web.dto.*;
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
 * 관리자 회원 관리 API
 */
@Tag(name = "1901. Admin - User", description = "관리자 회원 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 전체 회원 목록 조회
     */
    @Operation(summary = "[관리자] 전체 회원 목록 조회",
            description = "전체 회원 목록을 조회합니다.\n\n" +
                    "**검색 필터**\n" +
                    "- keyword: 이메일, 이름으로 검색 (선택)\n" +
                    "- status: 상태별 필터 (ACTIVE, INACTIVE, SUSPENDED, PENDING) (선택)\n" +
                    "- role: 역할별 필터 (USER, COMPANY, ADMIN) (선택)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: created_at DESC (최신순)\n" +
                    "- 사용법: sort=created_at,desc 또는 sort=created_at,asc\n" +
                    "- ⚠️ camelCase(createdAt) 사용 불가, snake_case(created_at) 사용")
    @GetMapping
    public ApiResponse<Page<AdminUserListResponse>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "created_at", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 전체 회원 목록 조회: adminEmail={}, keyword={}, status={}, role={}",
                userDetails.getUsername(), keyword, status, role);
        Page<AdminUserListResponse> response = adminUserService.getAllUsers(keyword, status, role, pageable);
        return ApiResponse.success(response);
    }

    /**
     * 회원 상세 정보 조회
     */
    @Operation(summary = "[관리자] 회원 상세 정보 조회",
            description = "회원의 상세 정보를 조회합니다.\n\n" +
                    "**포함 정보**\n" +
                    "- 기본 정보 (이메일, 이름, 전화번호)\n" +
                    "- 역할 및 상태\n" +
                    "- 인증 현황 (이메일, 휴대폰, 본인인증)\n" +
                    "- 약관 동의 현황\n" +
                    "- 로그인 정보")
    @GetMapping("/{userUuid}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userUuid) {
        log.info("[관리자] 회원 상세 정보 조회: adminEmail={}, userUuid={}",
                userDetails.getUsername(), userUuid);
        AdminUserDetailResponse response = adminUserService.getUserDetail(userUuid);
        return ApiResponse.success(response);
    }

    /**
     * 회원 상태 변경
     */
    @Operation(summary = "[관리자] 회원 상태 변경",
            description = "회원의 상태를 변경합니다.\n\n" +
                    "**상태 종류**\n" +
                    "- ACTIVE: 활성 (정상 사용 가능)\n" +
                    "- INACTIVE: 비활성 (탈퇴 등)\n" +
                    "- SUSPENDED: 정지 (이용 제한)\n" +
                    "- PENDING: 대기 (가입 승인 대기)")
    @PatchMapping("/{userUuid}/status")
    public ApiResponse<AdminUserDetailResponse> updateUserStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userUuid,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        log.info("[관리자] 회원 상태 변경: adminEmail={}, userUuid={}, newStatus={}",
                userDetails.getUsername(), userUuid, request.getStatus());
        AdminUserDetailResponse response = adminUserService.updateUserStatus(userUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 회원 역할 변경
     */
    @Operation(summary = "[관리자] 회원 역할 변경",
            description = "회원의 역할을 변경합니다.\n\n" +
                    "**역할 종류**\n" +
                    "- USER: 일반 사용자\n" +
                    "- COMPANY: 업체 회원\n" +
                    "- ADMIN: 관리자\n\n" +
                    "**참고**\n" +
                    "- 역할은 배열로 전달 (복수 역할 가능)\n" +
                    "- 예: [\"USER\", \"COMPANY\"]")
    @PatchMapping("/{userUuid}/roles")
    public ApiResponse<AdminUserDetailResponse> updateUserRoles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userUuid,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        log.info("[관리자] 회원 역할 변경: adminEmail={}, userUuid={}, newRoles={}",
                userDetails.getUsername(), userUuid, request.getRoles());
        AdminUserDetailResponse response = adminUserService.updateUserRoles(userUuid, request);
        return ApiResponse.success(response);
    }

    /**
     * 회원 삭제 (Soft Delete)
     */
    @Operation(summary = "[관리자] 회원 삭제",
            description = "회원을 삭제합니다.\n\n" +
                    "**참고**\n" +
                    "- Soft Delete 방식으로 처리됩니다\n" +
                    "- 실제 데이터는 삭제되지 않고 is_deleted 플래그만 변경됩니다")
    @DeleteMapping("/{userUuid}")
    public ApiResponse<Void> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userUuid) {
        log.info("[관리자] 회원 삭제: adminEmail={}, userUuid={}",
                userDetails.getUsername(), userUuid);
        adminUserService.deleteUser(userUuid);
        return ApiResponse.success();
    }
}
