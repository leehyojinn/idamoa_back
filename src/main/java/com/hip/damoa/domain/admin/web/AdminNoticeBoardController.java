package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.model.EventStatus;
import com.hip.damoa.domain.board.service.NoticeBoardService;
import com.hip.damoa.domain.board.web.dto.NoticeBoardRequest;
import com.hip.damoa.domain.board.web.dto.NoticeBoardResponse;
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
 * Notice/Event 게시판 관리 Controller (관리자용)
 *
 * 공지사항 및 이벤트 등록/수정/삭제 API (관리자 전용)
 */
@Tag(name = "9906. Admin - Notice/Event Board", description = "공지사항 및 이벤트 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeBoardController {

    private final NoticeBoardService noticeBoardService;

    // ==================== 공지사항 (NOTICE) ====================

    @Operation(summary = "공지사항 생성", description = "새로운 공지사항을 생성합니다 (관리자 전용)")
    @PostMapping("/notice")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeBoardResponse> createNotice(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoticeBoardRequest request) {

        log.info("공지사항 생성 요청 (관리자): userEmail={}", userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.createNotice(
                userDetails.getUsername(), BoardType.NOTICE.name(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다 (관리자 전용)")
    @PutMapping("/notice/{uuid}")
    public ApiResponse<NoticeBoardResponse> updateNotice(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoticeBoardRequest request) {

        log.info("공지사항 수정 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.updateNotice(
                uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다 (관리자 전용, Soft Delete)")
    @DeleteMapping("/notice/{uuid}")
    public ApiResponse<Void> deleteNotice(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("공지사항 삭제 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        noticeBoardService.deleteNotice(uuid, userDetails.getUsername());

        return ApiResponse.success();
    }

    // ==================== 이벤트 (EVENT) ====================

    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다 (관리자 전용)")
    @PostMapping("/event")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeBoardResponse> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoticeBoardRequest request) {

        log.info("이벤트 생성 요청 (관리자): userEmail={}", userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.createNotice(
                userDetails.getUsername(), BoardType.EVENT.name(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "이벤트 수정", description = "이벤트를 수정합니다 (관리자 전용)")
    @PutMapping("/event/{uuid}")
    public ApiResponse<NoticeBoardResponse> updateEvent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoticeBoardRequest request) {

        log.info("이벤트 수정 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.updateNotice(
                uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다 (관리자 전용, Soft Delete)")
    @DeleteMapping("/event/{uuid}")
    public ApiResponse<Void> deleteEvent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("이벤트 삭제 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        noticeBoardService.deleteNotice(uuid, userDetails.getUsername());

        return ApiResponse.success();
    }

    @Operation(summary = "이벤트 종료",
            description = "진행 중인 이벤트를 수동으로 종료합니다 (관리자 전용)\n\n" +
                    "**사용 시나리오:**\n" +
                    "- 조기 종료가 필요한 경우\n" +
                    "- 이벤트 문제 발생으로 긴급 종료가 필요한 경우")
    @PutMapping("/event/{uuid}/end")
    public ApiResponse<NoticeBoardResponse> endEvent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("이벤트 종료 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.updateEventStatus(
                uuid, userDetails.getUsername(), "ENDED");

        return ApiResponse.success(response);
    }

    @Operation(summary = "이벤트 활성화",
            description = "종료된 이벤트를 다시 활성화합니다 (관리자 전용)\n\n" +
                    "**사용 시나리오:**\n" +
                    "- 이벤트 기간 연장\n" +
                    "- 잘못 종료된 이벤트 복구")
    @PutMapping("/event/{uuid}/activate")
    public ApiResponse<NoticeBoardResponse> activateEvent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("이벤트 활성화 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        NoticeBoardResponse response = noticeBoardService.updateEventStatus(
                uuid, userDetails.getUsername(), "ACTIVE");

        return ApiResponse.success(response);
    }

    // ==================== 검색 API ====================

    @Operation(summary = "공지사항 검색 (관리자)",
            description = "공지사항을 검색합니다 (관리자 전용)\n\n" +
                    "**특징:**\n" +
                    "- 발행되지 않은 게시글도 조회 가능\n" +
                    "- 삭제된 게시글 제외\n" +
                    "- 고정 여부 필터링 가능\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping("/notice/search")
    public ApiResponse<Page<NoticeBoardResponse>> searchNotices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPinned,
            @RequestParam(required = false) Boolean isPublished,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("공지사항 검색 요청 (관리자): keyword={}, isPinned={}, isPublished={}",
                 keyword, isPinned, isPublished);

        Page<NoticeBoardResponse> response;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색
            response = noticeBoardService.searchNotice(BoardType.NOTICE.name(), keyword, null, pageable);
        } else {
            // 목록 조회
            response = noticeBoardService.getNoticeList(BoardType.NOTICE.name(), null, pageable);
        }

        return ApiResponse.success(response);
    }

    @Operation(summary = "이벤트 검색 (관리자)",
            description = "이벤트를 검색합니다 (관리자 전용)\n\n" +
                    "**특징:**\n" +
                    "- 발행되지 않은 게시글도 조회 가능\n" +
                    "- 삭제된 게시글 제외\n" +
                    "- 이벤트 상태 필터링 가능\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping("/event/search")
    public ApiResponse<Page<NoticeBoardResponse>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventStatus eventStatus,
            @RequestParam(required = false) Boolean isPinned,
            @RequestParam(required = false) Boolean isPublished,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("이벤트 검색 요청 (관리자): keyword={}, eventStatus={}, isPinned={}, isPublished={}",
                 keyword, eventStatus, isPinned, isPublished);

        Page<NoticeBoardResponse> response;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색
            response = noticeBoardService.searchNotice(BoardType.EVENT.name(), keyword, eventStatus, pageable);
        } else {
            // 목록 조회
            response = noticeBoardService.getNoticeList(BoardType.EVENT.name(), eventStatus, pageable);
        }

        return ApiResponse.success(response);
    }

    @Operation(summary = "공지사항/이벤트 통합 검색 (관리자)",
            description = "공지사항 및 이벤트를 통합 검색합니다 (관리자 전용)\n\n" +
                    "**특징:**\n" +
                    "- 모든 게시판 타입 통합 검색\n" +
                    "- 발행되지 않은 게시글도 조회 가능\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping("/notice-event/search")
    public ApiResponse<Page<NoticeBoardResponse>> searchAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(required = false) EventStatus eventStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String boardTypeStr = boardType != null ? boardType.name() : null;

        log.info("통합 검색 요청 (관리자): keyword={}, boardType={}, eventStatus={}",
                 keyword, boardTypeStr, eventStatus);

        Page<NoticeBoardResponse> response;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색
            response = noticeBoardService.searchNotice(boardTypeStr, keyword, eventStatus, pageable);
        } else {
            // 목록 조회
            response = noticeBoardService.getNoticeList(boardTypeStr, eventStatus, pageable);
        }

        return ApiResponse.success(response);
    }
}
