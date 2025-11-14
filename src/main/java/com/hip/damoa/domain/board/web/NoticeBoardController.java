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

    @Operation(summary = "공지사항/이벤트 조회",
            description = "특정 공지사항 또는 이벤트를 조회합니다.\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 제목, 내용\n" +
                    "- 게시판 타입 (NOTICE, EVENT, FAQ)\n" +
                    "- 작성자 정보\n" +
                    "- 조회수\n" +
                    "- 이벤트인 경우: 이벤트 상태 (ACTIVE, ENDED), 시작일, 종료일\n" +
                    "- 고정 여부 (isPinned)\n" +
                    "- 첨부파일 목록 (있는 경우)\n" +
                    "- 썸네일 이미지 URL (있는 경우)\n\n" +
                    "**조회수 증가:**\n" +
                    "- 게시글 조회 시 조회수 자동 증가\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 공지사항 상세 페이지\n" +
                    "- 이벤트 상세 페이지\n" +
                    "- FAQ 상세 페이지")
    @GetMapping("/notice-event/{uuid}")
    public ApiResponse<NoticeBoardResponse> getNoticeOrEvent(@PathVariable UUID uuid) {

        log.info("공지사항/이벤트 조회 요청: uuid={}", uuid);

        NoticeBoardResponse response = noticeBoardService.getNotice(uuid, null);

        return ApiResponse.success(response);
    }

    // ==================== 공지사항/이벤트 통합 검색 ====================

    @Operation(summary = "공지사항/이벤트 검색 (통합)",
            description = "공지사항 및 이벤트 검색/목록 조회 통합 API입니다.\n\n" +
                    "**검색 조건 (모두 선택):**\n" +
                    "- keyword: 제목, 내용에서 검색\n" +
                    "- boardType: 게시판 타입 필터\n" +
                    "  - `NOTICE`: 공지사항만\n" +
                    "  - `EVENT`: 이벤트만\n" +
                    "  - `FAQ`: FAQ만\n" +
                    "  - null: 전체 (공지사항 + 이벤트 + FAQ)\n" +
                    "- eventStatus: 이벤트 상태 필터 (boardType=EVENT인 경우만 유효)\n" +
                    "  - `ACTIVE`: 진행 중인 이벤트만\n" +
                    "  - `ENDED`: 종료된 이벤트만\n" +
                    "  - null: 전체 이벤트\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n" +
                    "- sort: 정렬 기준 (기본: publishedAt,DESC - 최신순)\n\n" +
                    "**응답:**\n" +
                    "- 게시글 목록 (제목, 썸네일, 작성자, 조회수 등)\n" +
                    "- 이벤트인 경우 이벤트 상태 및 기간 포함\n" +
                    "- 고정 게시글 (isPinned=true) 포함\n" +
                    "- 페이지 정보 (totalElements, totalPages 등)\n\n" +
                    "**정렬 옵션:**\n" +
                    "- publishedAt,DESC: 최신순 (기본값)\n" +
                    "- viewCount,DESC: 조회수 높은 순\n" +
                    "- createdAt,DESC: 생성일 최신순\n\n" +
                    "**활용:**\n" +
                    "- 공지사항 메인 페이지\n" +
                    "- 이벤트 목록 페이지\n" +
                    "- FAQ 목록 페이지\n" +
                    "- 검색 결과 페이지")
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

    @Operation(summary = "고정된 공지사항/이벤트 목록",
            description = "고정된 공지사항 및 이벤트 목록을 조회합니다.\n\n" +
                    "**필터 조건:**\n" +
                    "- boardType: 게시판 타입 필터\n" +
                    "  - `NOTICE`: 고정된 공지사항만\n" +
                    "  - `EVENT`: 고정된 이벤트만\n" +
                    "  - null: 모든 고정 게시글 (공지사항 + 이벤트)\n\n" +
                    "**응답:**\n" +
                    "- 고정 게시글 목록 (isPinned=true)\n" +
                    "- 최신순 정렬\n" +
                    "- 페이지네이션 없음 (전체 고정 게시글 반환)\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 메인 페이지 상단 고정 공지사항\n" +
                    "- 중요 이벤트 노출\n" +
                    "- 상단 고정 영역")
    @GetMapping("/notice-event/pinned")
    public ApiResponse<List<NoticeBoardResponse>> getPinnedNoticeEvents(
            @RequestParam(required = false) BoardType boardType) {

        String boardTypeStr = boardType != null ? boardType.name() : null;

        log.info("고정된 공지사항/이벤트 목록 조회 요청: boardType={}", boardTypeStr);

        List<NoticeBoardResponse> response = noticeBoardService.getPinnedNotices(boardTypeStr);

        return ApiResponse.success(response);
    }
}
