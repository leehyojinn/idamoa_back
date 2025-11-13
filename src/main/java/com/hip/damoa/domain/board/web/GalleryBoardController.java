package com.hip.damoa.domain.board.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.service.BoardBookmarkService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gallery 게시판 Controller
 *
 * 사진 게시판 API
 */
@Tag(name = "10. Gallery Board", description = "사진 게시판 API (BoardType: GALLERY) - 이미지 업로드, 필터 기능, 북마크 지원")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/gallery")
public class GalleryBoardController {

    private final GalleryBoardService galleryBoardService;
    private final BoardBookmarkService boardBookmarkService;

    @Operation(summary = "Gallery 게시글 생성", description = "새로운 사진 게시글을 생성합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GalleryResponse> createGallery(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GalleryCreateRequest request) {

        log.info("Gallery 게시글 생성 요청: userEmail={}", userDetails.getUsername());

        GalleryResponse response = galleryBoardService.createGallery(
                userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Gallery 게시글 조회", description = "특정 사진 게시글을 조회합니다 (조회수 증가)")
    @GetMapping("/{uuid}")
    public ApiResponse<GalleryResponse> getGallery(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails) {

        log.info("Gallery 게시글 조회 요청: uuid={}", uuid);

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        GalleryResponse response = galleryBoardService.getGallery(uuid, userEmail);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Gallery 게시글 검색 (통합)",
               description = "사진 게시판 검색/목록 조회 통합 API - 키워드, 필터 옵션, 북마크 필터링 지원")
    @GetMapping("/search")
    public ApiResponse<Page<GalleryResponse>> searchGalleries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> filterOptionIds,
            @RequestParam(required = false) Boolean onlyBookmarked,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("Gallery 게시글 검색 요청: keyword={}, filterOptionIds={}, onlyBookmarked={}, userEmail={}",
                 keyword, filterOptionIds, onlyBookmarked, userEmail);

        Page<GalleryResponse> response = galleryBoardService.searchGalleries(
                keyword, filterOptionIds, onlyBookmarked, userEmail, pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Gallery 게시글 수정", description = "사진 게시글을 수정합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{uuid}")
    public ApiResponse<GalleryResponse> updateGallery(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GalleryUpdateRequest request) {

        log.info("Gallery 게시글 수정 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        GalleryResponse response = galleryBoardService.updateGallery(
                uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Gallery 게시글 삭제", description = "사진 게시글을 삭제합니다 (Soft Delete)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteGallery(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Gallery 게시글 삭제 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        galleryBoardService.deleteGallery(uuid, userDetails.getUsername());

        return ApiResponse.success();
    }

//    @Operation(summary = "Featured Gallery 목록", description = "추천 사진 게시글 목록을 조회합니다")
//    @GetMapping("/featured")
//    public ApiResponse<List<GalleryResponse>> getFeaturedGalleries() {
//
//        log.info("Featured Gallery 목록 조회 요청");
//
//        List<GalleryResponse> response = galleryBoardService.getFeaturedGalleries();
//
//        return ApiResponse.success(response);
//    }
//
//    @Operation(summary = "Pinned Gallery 목록", description = "고정된 사진 게시글 목록을 조회합니다")
//    @GetMapping("/pinned")
//    public ApiResponse<List<GalleryResponse>> getPinnedGalleries() {
//
//        log.info("Pinned Gallery 목록 조회 요청");
//
//        List<GalleryResponse> response = galleryBoardService.getPinnedGalleries();
//
//        return ApiResponse.success(response);
//    }

    @Operation(summary = "Gallery 북마크 토글", description = "사진 게시글 북마크를 추가/제거합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> toggleBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Gallery 북마크 토글 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.toggleBookmark(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }

    @Operation(summary = "Gallery 북마크 여부 확인", description = "사용자의 북마크 여부를 확인합니다")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> checkBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Gallery 북마크 확인 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.isBookmarked(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }
}
