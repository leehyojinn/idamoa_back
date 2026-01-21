package com.hip.damoa.domain.board.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.filter.service.FilterService;
import com.hip.damoa.domain.filter.web.dto.BoardFilterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시판 필터 조회 Controller
 *
 * 게시판 타입별로 사용 가능한 필터를 동적으로 제공합니다.
 * DB의 metadata.board_types 배열을 기준으로 필터가 결정됩니다.
 *
 * Note: 사진 게시판(Gallery) 필터 기능은 Portfolio 도메인으로 이관됨
 */
@Tag(name = "1010-1. Board Filter", description = "게시판 필터 조회 API (동적 필터 제공)")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/filters")
public class BoardFilterController {

    private final FilterService filterService;

    /**
     * 자료실 게시판용 필터 목록 조회
     */
    @Operation(
        summary = "자료실 필터 조회",
        description = """
            자료실 게시판에서 사용 가능한 필터 목록을 조회합니다.

            **필터 동작 방식:**
            - DB의 filter_categories 테이블에서 entity_type='BOARD'인 카테고리 조회
            - metadata.board_types 배열에 'DOCUMENT'가 포함된 카테고리만 반환
            - 각 카테고리별 활성화된 옵션들이 계층 구조로 포함됨

            **반환되는 필터 구조:**
            - 카테고리 정보 (code, name, description)
            - 선택 가능한 옵션들 (옵션별 id, uuid, code, name 포함)
            - 다중 선택 가능 여부 (multiSelectable)

            필터는 DB 설정에 따라 동적으로 변경될 수 있습니다.
            """
    )
    @GetMapping("/document")
    public ApiResponse<List<BoardFilterResponse>> getDocumentFilters() {
        log.info("자료실 필터 목록 조회");

        List<BoardFilterResponse> filters = filterService.getDocumentFilters();

        log.info("자료실 필터 조회 완료: {}개 카테고리", filters.size());
        return ApiResponse.success(filters);
    }

    /**
     * 게시판 타입별 필터 목록 조회
     */
    @Operation(
        summary = "게시판 타입별 필터 조회",
        description = """
            게시판 타입(DOCUMENT)에 따른 필터 목록을 동적으로 조회합니다.

            **지원 타입:**
            - DOCUMENT: 자료실 게시판 (기본값)

            **필터 동작 방식:**
            - 전달받은 boardType을 기준으로 필터 카테고리 조회
            - DB의 metadata.board_types 배열에서 해당 타입이 포함된 카테고리만 반환
            - 필터 카테고리와 옵션은 DB에서 동적으로 관리됨

            Note: 사진 게시판(Gallery) 필터는 /api/portfolios 도메인에서 제공됩니다.
            """
    )
    @GetMapping
    public ApiResponse<List<BoardFilterResponse>> getBoardFilters(
            @RequestParam(required = false, defaultValue = "DOCUMENT") String boardType) {

        log.info("게시판 필터 목록 조회: boardType={}", boardType);

        List<BoardFilterResponse> filters = filterService.getDocumentFilters();

        log.info("게시판 필터 조회 완료: boardType={}, {}개 카테고리", boardType, filters.size());
        return ApiResponse.success(filters);
    }
}
