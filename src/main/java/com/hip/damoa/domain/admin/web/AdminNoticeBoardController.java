package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.service.NoticeBoardService;
import com.hip.damoa.domain.board.web.dto.NoticeBoardRequest;
import com.hip.damoa.domain.board.web.dto.NoticeBoardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Notice/Event 게시판 관리 Controller (관리자용)
 *
 * 공지사항 및 이벤트 등록/수정/삭제 API (관리자 전용)
 */
@Tag(name = "1904. Admin - Notice/Event Board", description = "공지사항 및 이벤트 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteEvent(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("이벤트 삭제 요청 (관리자): uuid={}, userEmail={}", uuid, userDetails.getUsername());

        noticeBoardService.deleteNotice(uuid, userDetails.getUsername());

        return ApiResponse.success();
    }
}
