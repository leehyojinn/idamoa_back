package com.hip.damoa.domain.board.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.service.BoardBookmarkService;
import com.hip.damoa.domain.board.service.DocumentBoardService;
import com.hip.damoa.domain.board.web.dto.DocumentCreateRequest;
import com.hip.damoa.domain.board.web.dto.DocumentResponse;
import com.hip.damoa.domain.board.web.dto.DocumentUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

/**
 * Document 게시판 Controller
 *
 * 자료실 API
 */
@Tag(name = "11. Document Board", description = "자료실 API (BoardType: DOCUMENT) - 파일 업로드/다운로드, 필터 기능, 북마크 지원, 유료 파일 지원")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/document")
public class DocumentBoardController {

    private final DocumentBoardService documentBoardService;
    private final BoardBookmarkService boardBookmarkService;

    @Operation(summary = "Document 게시글 생성", description = "새로운 자료실 게시글을 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> createDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentCreateRequest request) {

        log.info("Document 게시글 생성 요청: userEmail={}", userDetails.getUsername());

        DocumentResponse response = documentBoardService.createDocument(
                userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 조회", description = "특정 자료실 게시글을 조회합니다 (조회수 증가)")
    @GetMapping("/{uuid}")
    public ApiResponse<DocumentResponse> getDocument(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails) {

        log.info("Document 게시글 조회 요청: uuid={}", uuid);

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        DocumentResponse response = documentBoardService.getDocument(uuid, userEmail);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 검색 (통합)",
               description = "자료실 검색/목록 조회 통합 API - 키워드, 필터 옵션, 북마크 필터링 지원")
    @GetMapping("/search")
    public ApiResponse<Page<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> filterOptionIds,
            @RequestParam(required = false) Boolean onlyBookmarked,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("Document 게시글 검색 요청: keyword={}, filterOptionIds={}, onlyBookmarked={}, userEmail={}",
                 keyword, filterOptionIds, onlyBookmarked, userEmail);

        Page<DocumentResponse> response = documentBoardService.searchDocuments(
                keyword, filterOptionIds, onlyBookmarked, userEmail, pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 수정", description = "자료실 게시글을 수정합니다")
    @PutMapping("/{uuid}")
    public ApiResponse<DocumentResponse> updateDocument(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentUpdateRequest request) {

        log.info("Document 게시글 수정 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        DocumentResponse response = documentBoardService.updateDocument(
                uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 삭제", description = "자료실 게시글을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteDocument(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Document 게시글 삭제 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        documentBoardService.deleteDocument(uuid, userDetails.getUsername());

        return ApiResponse.success();
    }

//    @Operation(summary = "Featured Document 목록", description = "추천 자료실 게시글 목록을 조회합니다")
//    @GetMapping("/featured")
//    public ApiResponse<List<DocumentResponse>> getFeaturedDocuments() {
//
//        log.info("Featured Document 목록 조회 요청");
//
//        List<DocumentResponse> response = documentBoardService.getFeaturedDocuments();
//
//        return ApiResponse.success(response);
//    }
//
//    @Operation(summary = "Pinned Document 목록", description = "고정된 자료실 게시글 목록을 조회합니다")
//    @GetMapping("/pinned")
//    public ApiResponse<List<DocumentResponse>> getPinnedDocuments() {
//
//        log.info("Pinned Document 목록 조회 요청");
//
//        List<DocumentResponse> response = documentBoardService.getPinnedDocuments();
//
//        return ApiResponse.success(response);
//    }

    @Operation(summary = "Document 북마크 토글", description = "자료실 게시글 북마크를 추가/제거합니다")
    @PostMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> toggleBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Document 북마크 토글 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.toggleBookmark(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }

    @Operation(summary = "Document 북마크 여부 확인", description = "사용자의 북마크 여부를 확인합니다")
    @GetMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> checkBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Document 북마크 확인 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.isBookmarked(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }
}
