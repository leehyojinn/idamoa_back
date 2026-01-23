package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.popup.service.PopupService;
import com.hip.damoa.domain.popup.web.dto.PopupCreateRequest;
import com.hip.damoa.domain.popup.web.dto.PopupResponse;
import com.hip.damoa.domain.popup.web.dto.PopupUpdateRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 팝업 관리 Controller (관리자 전용)
 *
 * 홈페이지 팝업 등록/수정/삭제 API (ADMIN 전용)
 */
@Tag(name = "9910. Admin - Popup", description = "팝업 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")  // 관리자만 접근 가능
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/popups")
public class AdminPopupController {

    private final PopupService popupService;

    @Operation(summary = "팝업 생성",
            description = "새로운 팝업을 생성합니다 (관리자 전용).\n\n" +
                    "**이미지 업로드 프로세스:**\n" +
                    "1. `/api/files/presigned` 호출 → Presigned URL 획득\n" +
                    "2. S3로 이미지 직접 업로드 (PUT 요청)\n" +
                    "3. `/api/files/complete` 호출 → 파일 UUID 획득\n" +
                    "4. **이 API 호출** → 획득한 파일 UUID를 imageUuid로 전송\n\n" +
                    "**필수 정보:**\n" +
                    "- title: 팝업 제목 (최대 200자)\n\n" +
                    "**선택 정보:**\n" +
                    "- content: 팝업 내용 (HTML 지원, 최대 10000자)\n" +
                    "- imageUuid: 팝업 이미지 UUID (files 테이블)\n" +
                    "- linkUrl: 클릭 시 이동할 URL (최대 500자)\n" +
                    "- displayStartDate: 노출 시작일시 (null이면 제한 없음)\n" +
                    "- displayEndDate: 노출 종료일시 (null이면 제한 없음)\n" +
                    "- displayOrder: 노출 순서 (낮을수록 먼저 표시, 기본값 0)\n" +
                    "- isActive: 활성화 여부 (기본값 true)\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수\n\n" +
                    "**활용:**\n" +
                    "- 홈페이지 팝업 등록\n" +
                    "- 이벤트 팝업 생성\n" +
                    "- 공지 팝업 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PopupResponse> createPopup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PopupCreateRequest request) {

        log.info("팝업 생성 요청 (관리자): userEmail={}", userDetails.getUsername());

        PopupResponse response = popupService.createPopup(userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 수정",
            description = "팝업을 수정합니다 (관리자 전용).\n\n" +
                    "**수정 가능 정보:**\n" +
                    "- title: 팝업 제목\n" +
                    "- content: 팝업 내용\n" +
                    "- imageUuid: 팝업 이미지 UUID\n" +
                    "- linkUrl: 클릭 시 이동할 URL\n" +
                    "- displayStartDate: 노출 시작일시\n" +
                    "- displayEndDate: 노출 종료일시\n" +
                    "- displayOrder: 노출 순서\n\n" +
                    "**참고:**\n" +
                    "- isActive는 별도의 활성화/비활성화 API 사용\n" +
                    "- 조회수, 클릭수는 수정 불가\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @PutMapping("/{uuid}")
    public ApiResponse<PopupResponse> updatePopup(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PopupUpdateRequest request) {

        log.info("팝업 수정 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        PopupResponse response = popupService.updatePopup(uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 삭제",
            description = "팝업을 삭제합니다 (관리자 전용, Soft Delete).\n\n" +
                    "**Soft Delete:**\n" +
                    "- 실제로 데이터베이스에서 삭제되지 않음\n" +
                    "- is_deleted 플래그만 true로 변경\n" +
                    "- deleted_at 필드에 삭제 시간 기록\n" +
                    "- 복구 가능 (별도 API 필요)\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePopup(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("팝업 삭제 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        popupService.deletePopup(uuid);

        return ApiResponse.success();
    }

    @Operation(summary = "팝업 조회",
            description = "특정 팝업을 조회합니다 (관리자 전용).\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 팝업 기본 정보 (제목, 내용, 링크 등)\n" +
                    "- 이미지 UUID 및 URL\n" +
                    "- 노출 기간 (시작일, 종료일)\n" +
                    "- 노출 순서, 활성화 여부\n" +
                    "- 조회수, 클릭수\n" +
                    "- 생성자, 수정자 정보\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @GetMapping("/{uuid}")
    public ApiResponse<PopupResponse> getPopup(@PathVariable UUID uuid) {

        log.info("팝업 조회 요청 (관리자): uuid={}", uuid);

        PopupResponse response = popupService.getPopup(uuid);

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 목록 조회",
            description = "팝업 목록을 조회합니다 (관리자 전용, 페이징).\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n\n" +
                    "## 정렬\n" +
                    "- 기본값: displayOrder ASC (순서 오름차순)\n" +
                    "- 사용법: sort=displayOrder,asc 또는 sort=displayOrder,desc\n" +
                    "- 기타 옵션: createdAt, viewCount, clickCount\n\n" +
                    "**응답:**\n" +
                    "- 삭제되지 않은 팝업만 조회\n" +
                    "- 활성/비활성 모두 포함\n" +
                    "- 페이지 정보 (totalElements, totalPages 등)\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @GetMapping
    public ApiResponse<Page<PopupResponse>> getPopupList(
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.info("팝업 목록 조회 요청 (관리자): pageable={}", pageable);

        Page<PopupResponse> response = popupService.getPopupList(pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 활성화",
            description = "팝업을 활성화합니다 (관리자 전용).\n\n" +
                    "**동작:**\n" +
                    "- isActive 플래그를 true로 변경\n" +
                    "- 활성화된 팝업은 홈페이지에 노출됨\n" +
                    "- 단, displayStartDate/displayEndDate 기간 내에서만 노출\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @PatchMapping("/{uuid}/activate")
    public ApiResponse<PopupResponse> activatePopup(@PathVariable UUID uuid) {

        log.info("팝업 활성화 요청 (관리자): uuid={}", uuid);

        PopupResponse response = popupService.activatePopup(uuid);

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 비활성화",
            description = "팝업을 비활성화합니다 (관리자 전용).\n\n" +
                    "**동작:**\n" +
                    "- isActive 플래그를 false로 변경\n" +
                    "- 비활성화된 팝업은 홈페이지에 노출되지 않음\n" +
                    "- 삭제와 다르게 언제든지 다시 활성화 가능\n\n" +
                    "**권한:**\n" +
                    "- ADMIN 역할 필요\n" +
                    "- 로그인 필수")
    @PatchMapping("/{uuid}/deactivate")
    public ApiResponse<PopupResponse> deactivatePopup(@PathVariable UUID uuid) {

        log.info("팝업 비활성화 요청 (관리자): uuid={}", uuid);

        PopupResponse response = popupService.deactivatePopup(uuid);

        return ApiResponse.success(response);
    }
}
