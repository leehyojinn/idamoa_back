package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.board.service.BoardService;
import com.hip.damoa.domain.board.service.GalleryBoardService;
import com.hip.damoa.domain.board.web.dto.GalleryCreateRequest;
import com.hip.damoa.domain.board.web.dto.GalleryResponse;
import com.hip.damoa.domain.board.web.dto.GalleryUpdateRequest;
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
 * 관리자 사진 관리 API
 */
@Tag(name = "1907. Admin - Gallery Board", description = "관리자 사진 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards/gallery")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGalleryBoardController {

    private final GalleryBoardService galleryBoardService;
    private final BoardService boardService;
    private final BoardRepository boardRepository;

    /**
     * 사진 게시글 목록 조회 (관리자용 - 미게시 포함)
     */
    @Operation(summary = "[관리자] 사진 게시글 목록 조회",
            description = "사진 게시글 목록을 조회합니다 (미게시 포함).\n\n" +
                    "**검색 필터**\n" +
                    "- keyword: 제목/내용으로 검색 (선택)\n" +
                    "- filterOptionIds: 필터 옵션 ID 배열 (선택)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: publishedAt DESC (최신순)\n" +
                    "- 사용법: sort=publishedAt,desc 또는 sort=publishedAt,asc\n" +
                    "- 기타 옵션: createdAt, viewCount, likeCount")
    @GetMapping
    public ApiResponse<Page<GalleryResponse>> getAllGalleries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 사진 게시글 목록 조회: adminEmail={}, keyword={}",
                userDetails.getUsername(), keyword);
        // 관리자는 미게시 포함 전체 조회
        Page<GalleryResponse> response = galleryBoardService.searchGalleries(
                keyword, null, null, false, false, userDetails.getUsername(), null, pageable);
        return ApiResponse.success(response);
    }

    /**
     * 사진 게시글 상세 조회
     */
    @Operation(summary = "[관리자] 사진 게시글 상세 조회",
            description = "사진 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{galleryUuid}")
    public ApiResponse<GalleryResponse> getGalleryDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid) {
        log.info("[관리자] 사진 게시글 상세 조회: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);
        GalleryResponse response = galleryBoardService.getGallery(galleryUuid, userDetails.getUsername());
        return ApiResponse.success(response);
    }

    /**
     * 사진 게시글 생성
     */
    @Operation(summary = "[관리자] 사진 게시글 생성",
            description = "새로운 사진 게시글을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GalleryResponse> createGallery(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GalleryCreateRequest request) {
        log.info("[관리자] 사진 게시글 생성: adminEmail={}, title={}",
                userDetails.getUsername(), request.getTitle());
        GalleryResponse response = galleryBoardService.createGallery(userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * 사진 게시글 수정
     */
    @Operation(summary = "[관리자] 사진 게시글 수정",
            description = "사진 게시글을 수정합니다.")
    @PutMapping("/{galleryUuid}")
    public ApiResponse<GalleryResponse> updateGallery(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid,
            @Valid @RequestBody GalleryUpdateRequest request) {
        log.info("[관리자] 사진 게시글 수정: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);
        GalleryResponse response = galleryBoardService.updateGallery(galleryUuid, userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * 사진 게시글 삭제
     */
    @Operation(summary = "[관리자] 사진 게시글 삭제",
            description = "사진 게시글을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{galleryUuid}")
    public ApiResponse<Void> deleteGallery(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid) {
        log.info("[관리자] 사진 게시글 삭제: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);
        Board board = boardService.getBoard(galleryUuid);
        board.softDelete();
        return ApiResponse.success();
    }

    /**
     * 사진 게시글 게시/게시 취소 토글
     */
    @Operation(summary = "[관리자] 사진 게시글 게시/취소",
            description = "사진 게시글의 게시 상태를 변경합니다.")
    @PatchMapping("/{galleryUuid}/publish")
    public ApiResponse<Void> togglePublish(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid) {
        log.info("[관리자] 사진 게시 상태 변경: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);

        Board board = boardService.getBoard(galleryUuid);
        if (board.getIsPublished()) {
            board.unpublish();
        } else {
            board.publish();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }

    /**
     * 사진 게시글 고정/고정 해제 토글
     */
    @Operation(summary = "[관리자] 사진 게시글 고정/해제",
            description = "사진 게시글의 고정 상태를 변경합니다.")
    @PatchMapping("/{galleryUuid}/pin")
    public ApiResponse<Void> togglePin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid) {
        log.info("[관리자] 사진 고정 상태 변경: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);

        Board board = boardService.getBoard(galleryUuid);
        if (board.getIsPinned()) {
            board.unpin();
        } else {
            board.pin();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }

    /**
     * 사진 게시글 추천/추천 해제 토글
     */
    @Operation(summary = "[관리자] 사진 게시글 추천/해제",
            description = "사진 게시글의 추천 상태를 변경합니다.")
    @PatchMapping("/{galleryUuid}/feature")
    public ApiResponse<Void> toggleFeature(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID galleryUuid) {
        log.info("[관리자] 사진 추천 상태 변경: adminEmail={}, galleryUuid={}",
                userDetails.getUsername(), galleryUuid);

        Board board = boardService.getBoard(galleryUuid);
        if (board.getIsFeatured()) {
            board.unfeature();
        } else {
            board.feature();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }
}
