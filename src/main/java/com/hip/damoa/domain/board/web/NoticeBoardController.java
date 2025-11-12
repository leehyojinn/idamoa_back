package com.hip.damoa.domain.board.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.model.EventStatus;
import com.hip.damoa.domain.board.service.NoticeBoardService;
import com.hip.damoa.domain.board.web.dto.NoticeBoardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Notice/Event 게시판 Controller (일반 사용자용)
 *
 * 공지사항 및 이벤트 조회 API
 */
@Tag(name = "12. Notice/Event Board", description = "공지사항 및 이벤트 API (BoardType: NOTICE, EVENT, FAQ) - 조회 전용")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class NoticeBoardController {

    private final NoticeBoardService noticeBoardService;

    // ==================== 공지사항/이벤트 단건 조회 ====================

    @Operation(summary = "공지사항/이벤트 조회", description = "특정 공지사항 또는 이벤트를 조회합니다 (조회수 증가)")
    @GetMapping("/notice-event/{uuid}")
    public ApiResponse<NoticeBoardResponse> getNoticeOrEvent(@PathVariable UUID uuid) {

        log.info("공지사항/이벤트 조회 요청: uuid={}", uuid);

        NoticeBoardResponse response = noticeBoardService.getNotice(uuid, null);

        return ApiResponse.success(response);
    }

    // ==================== 공지사항/이벤트 통합 검색 ====================

    @Operation(summary = "공지사항/이벤트 검색 (통합)",
               description = "공지사항 및 이벤트 검색/목록 조회 통합 API - 키워드 검색, BoardType 필터, EventStatus 필터 지원\n\n" +
                          "**BoardType**: NOTICE(공지사항), EVENT(이벤트), null(전체)\n" +
                          "**EventStatus**: ACTIVE(진행중 이벤트), ENDED(종료된 이벤트), null(전체)")
    @GetMapping("/notice-event/search")
    public ApiResponse<Page<NoticeBoardResponse>> searchNoticeEvent(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(required = false) EventStatus eventStatus,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String boardTypeStr = boardType != null ? boardType.name() : null;

        log.info("공지사항/이벤트 검색 요청: keyword={}, boardType={}, eventStatus={}",
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

    @Operation(summary = "고정된 공지사항/이벤트 목록", description = "고정된 공지사항 및 이벤트 목록을 조회합니다")
    @GetMapping("/notice-event/pinned")
    public ApiResponse<List<NoticeBoardResponse>> getPinnedNoticeEvents(
            @RequestParam(required = false) BoardType boardType) {

        String boardTypeStr = boardType != null ? boardType.name() : null;

        log.info("고정된 공지사항/이벤트 목록 조회 요청: boardType={}", boardTypeStr);

        List<NoticeBoardResponse> response = noticeBoardService.getPinnedNotices(boardTypeStr);

        return ApiResponse.success(response);
    }
}
