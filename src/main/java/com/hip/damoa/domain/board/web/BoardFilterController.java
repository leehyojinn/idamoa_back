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
 * 갤러리/자료실 게시판용 필터 목록을 제공합니다.
 */
@Tag(name = "12-1. Board Filter", description = "게시판 필터 조회 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/filters")
public class BoardFilterController {

    private final FilterService filterService;

    /**
     * 갤러리 게시판용 필터 목록 조회
     */
    @Operation(
        summary = "갤러리 필터 조회",
        description = """
            갤러리 게시판에서 사용 가능한 필터 목록을 조회합니다.

            **포함되는 필터 카테고리:**
            - 평수 (board_area)
            - 스타일 (board_style)
            - 컬러 (board_color)
            - 공간별 (board_space_type)
            - 자재 (board_material)
            - 이미지 유형 (board_image_type)
            - 진료과목 (board_medical_specialty)

            각 카테고리별로 선택 가능한 옵션들이 포함되어 반환됩니다.
            """
    )
    @GetMapping("/gallery")
    public ApiResponse<List<BoardFilterResponse>> getGalleryFilters() {
        log.info("갤러리 필터 목록 조회");

        List<BoardFilterResponse> filters = filterService.getGalleryFilters();

        log.info("갤러리 필터 조회 완료: {}개 카테고리", filters.size());
        return ApiResponse.success(filters);
    }

    /**
     * 자료실 게시판용 필터 목록 조회
     */
    @Operation(
        summary = "자료실 필터 조회",
        description = """
            자료실 게시판에서 사용 가능한 필터 목록을 조회합니다.

            **포함되는 필터 카테고리:**
            - 평수 (board_area)
            - 문서 분류 (board_document_category)
            - 진료과목 (board_medical_specialty)

            각 카테고리별로 선택 가능한 옵션들이 포함되어 반환됩니다.
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
            게시판 타입(GALLERY/DOCUMENT)에 따른 필터 목록을 조회합니다.

            **지원 타입:**
            - GALLERY: 갤러리 게시판
            - DOCUMENT: 자료실 게시판
            """
    )
    @GetMapping
    public ApiResponse<List<BoardFilterResponse>> getBoardFilters(
            @RequestParam(required = false, defaultValue = "GALLERY") String boardType) {

        log.info("게시판 필터 목록 조회: boardType={}", boardType);

        List<BoardFilterResponse> filters;

        if ("DOCUMENT".equalsIgnoreCase(boardType)) {
            filters = filterService.getDocumentFilters();
        } else {
            filters = filterService.getGalleryFilters();
        }

        log.info("게시판 필터 조회 완료: boardType={}, {}개 카테고리", boardType, filters.size());
        return ApiResponse.success(filters);
    }
}