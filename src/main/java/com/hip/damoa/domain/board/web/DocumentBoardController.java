package com.hip.damoa.domain.board.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.service.BoardBookmarkService;
import com.hip.damoa.domain.board.service.DocumentBoardService;
import com.hip.damoa.domain.board.web.dto.DocumentCreateRequest;
import com.hip.damoa.domain.board.web.dto.DocumentResponse;
import com.hip.damoa.domain.board.web.dto.DocumentUpdateRequest;
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

    @Operation(summary = "Document 게시글 생성",
            description = "새로운 자료실 게시글을 생성합니다.\n\n" +
                    "**파일 업로드 프로세스:**\n" +
                    "1. `/api/files/presigned` 호출 → Presigned URL 획득\n" +
                    "2. S3로 파일 직접 업로드 (PUT 요청)\n" +
                    "3. `/api/files/complete` 호출 → 파일 UUID 획득\n" +
                    "4. **이 API 호출** → 획득한 파일 UUID를 fileUuids 배열로 전송\n\n" +
                    "**필수 정보:**\n" +
                    "- title: 제목 (최대 200자)\n" +
                    "- content: 내용 (최대 5000자)\n" +
                    "- fileUuids: 파일 UUID 배열 (최소 1개 이상)\n\n" +
                    "**선택 정보:**\n" +
                    "- categoryId: 카테고리 ID\n" +
                    "- thumbnailUuid: 썸네일 이미지 UUID (미리보기용)\n" +
                    "- isPaid: 유료 파일 여부 (기본: false)\n" +
                    "- price: 가격 (원) - 유료인 경우만 입력\n" +
                    "- filterOptionIds: 필터 옵션 ID 배열\n" +
                    "- tags: 태그 배열\n" +
                    "- isPublished: 즉시 게시 여부 (기본: true)\n" +
                    "- isPrivate: 비공개 여부 (기본: false)\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**유료 파일:**\n" +
                    "- isPaid=true 설정 시 결제 후 다운로드 가능\n" +
                    "- price 필드에 금액 설정 (KRW)\n" +
                    "- 결제 연동 기능 필요")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> createDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentCreateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("Document 게시글 생성 요청: userEmail={}", userDetails.getUsername());

        DocumentResponse response = documentBoardService.createDocument(
                userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 조회",
            description = "특정 자료실 게시글을 조회합니다.\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 제목, 내용, 파일 목록\n" +
                    "- 작성자 정보 (이름, 프로필 이미지)\n" +
                    "- 조회수, 좋아요 수, 북마크 수, 다운로드 수\n" +
                    "- 썸네일 이미지 URL (있는 경우)\n" +
                    "- 유료 파일 여부 및 가격\n" +
                    "- 필터 옵션 정보\n" +
                    "- 태그 목록\n" +
                    "- 로그인한 경우: 북마크 여부 포함\n\n" +
                    "**조회수 증가:**\n" +
                    "- 게시글 조회 시 조회수 자동 증가\n" +
                    "- 동일 사용자 중복 조회도 카운트됨\n\n" +
                    "**파일 다운로드:**\n" +
                    "- 무료 파일: 누구나 다운로드 가능\n" +
                    "- 유료 파일: 결제 후 다운로드 가능\n\n" +
                    "**권한:**\n" +
                    "- 비로그인 사용자도 조회 가능\n" +
                    "- 비공개 게시글은 작성자만 조회 가능")
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
            description = "자료실 검색 및 목록 조회 통합 API입니다.\n\n" +
                    "**검색 조건 (모두 선택적, 복합 검색 가능):**\n" +
                    "- keyword: 제목, 내용, 태그에서 검색\n" +
                    "- filterOptionIds: 필터 옵션 ID 배열 (예: 파일 형식, 카테고리)\n" +
                    "- onlyBookmarked: true 설정 시 북마크한 게시글만 조회 (로그인 필요)\n" +
                    "- onlyMyPosts: true 설정 시 내가 작성한 게시글만 조회 (로그인 필요)\n\n" +
                    "**복합 검색 예시:**\n" +
                    "- keyword + filterOptionIds: 특정 키워드와 필터 옵션을 모두 만족하는 게시글\n" +
                    "- keyword + onlyMyPosts: 내가 작성한 게시글 중 키워드를 포함하는 게시글\n" +
                    "- 모든 조건 조합 가능 (AND 연산)\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n" +
                    "- sort: 정렬 기준 (기본: publishedAt,DESC - 최신순)\n\n" +
                    "**응답:**\n" +
                    "- 게시글 목록 (제목, 썸네일, 작성자, 조회수, 다운로드 수 등)\n" +
                    "- 유료 파일 여부 및 가격 표시\n" +
                    "- 로그인한 경우 각 게시글의 북마크 여부 포함\n" +
                    "- 페이지 정보 (totalElements, totalPages 등)\n\n" +
                    "**정렬 옵션:**\n" +
                    "- publishedAt,DESC: 최신순 (기본값)\n" +
                    "- viewCount,DESC: 조회수 높은 순\n" +
                    "- downloadCount,DESC: 다운로드 많은 순\n" +
                    "- createdAt,DESC: 생성일 최신순\n\n" +
                    "**활용:**\n" +
                    "- 자료실 메인 페이지\n" +
                    "- 필터링된 자료 목록\n" +
                    "- 내가 북마크한 자료\n" +
                    "- 내가 작성한 자료")
    @GetMapping("/search")
    public ApiResponse<Page<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> filterOptionIds,
            @RequestParam(required = false) Boolean onlyBookmarked,
            @RequestParam(required = false) Boolean onlyMyPosts,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("Document 게시글 검색 요청: keyword={}, filterOptionIds={}, onlyBookmarked={}, onlyMyPosts={}, userEmail={}",
                 keyword, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail);

        Page<DocumentResponse> response = documentBoardService.searchDocuments(
                keyword, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail, pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "내가 작성한 Document 목록 (마이페이지)",
            description = "로그인한 사용자가 작성한 자료실 게시글 목록을 조회합니다.\n\n" +
                    "**조회 대상:**\n" +
                    "- 본인이 작성한 Document 게시글만 조회\n" +
                    "- 삭제되지 않은 게시글만 포함\n" +
                    "- 공개/비공개 상태 모두 포함\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n" +
                    "- sort: 정렬 기준 (기본: createdAt,DESC - 최신 작성순)\n\n" +
                    "**응답 정보:**\n" +
                    "- 게시글 목록 (제목, 파일, 조회수, 다운로드 수 등)\n" +
                    "- 북마크 여부 및 다운로드 여부 포함\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**활용:**\n" +
                    "- 마이페이지 - 내가 올린 자료\n" +
                    "- 자료 관리\n" +
                    "- 게시글 수정/삭제 전 목록 확인")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ApiResponse<Page<DocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("내 Document 목록 조회 요청: userEmail={}", userDetails.getUsername());

        Page<DocumentResponse> response = documentBoardService.getMyDocuments(
                userDetails.getUsername(), pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 수정",
            description = "자료실 게시글을 수정합니다.\n\n" +
                    "**수정 가능 항목:**\n" +
                    "- 제목, 내용\n" +
                    "- 파일 목록 (추가/삭제 가능)\n" +
                    "- 썸네일 이미지\n" +
                    "- 카테고리\n" +
                    "- 유료 파일 설정 (가격 변경 가능)\n" +
                    "- 필터 옵션\n" +
                    "- 태그\n" +
                    "- 공개/비공개 설정\n\n" +
                    "**권한:**\n" +
                    "- 작성자 본인만 수정 가능\n" +
                    "- 다른 사용자가 수정 시도 시 403 Forbidden\n\n" +
                    "**주의사항:**\n" +
                    "- 수정 시 updatedAt 자동 갱신\n" +
                    "- 파일 변경 시 새로운 파일 업로드 후 UUID 전송\n" +
                    "- 유료 파일 가격 변경 시 기존 구매자에게는 영향 없음")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{uuid}")
    public ApiResponse<DocumentResponse> updateDocument(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentUpdateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("Document 게시글 수정 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        DocumentResponse response = documentBoardService.updateDocument(
                uuid, userDetails.getUsername(), request);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Document 게시글 삭제",
            description = "자료실 게시글을 삭제합니다.\n\n" +
                    "**삭제 방식:**\n" +
                    "- Soft Delete (isDeleted=true 설정)\n" +
                    "- 실제 데이터는 DB에 남아있음\n" +
                    "- 목록 조회 시 노출되지 않음\n" +
                    "- S3 파일은 즉시 삭제되지 않음\n\n" +
                    "**권한:**\n" +
                    "- 작성자 본인만 삭제 가능\n" +
                    "- 다른 사용자가 삭제 시도 시 403 Forbidden\n\n" +
                    "**삭제 후:**\n" +
                    "- deletedAt 자동 설정\n" +
                    "- 복구 불가 (UI에서 접근 불가)\n" +
                    "- 연결된 북마크 정보는 유지됨\n" +
                    "- 유료 파일인 경우 기존 구매자는 계속 다운로드 가능")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteDocument(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

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

    @Operation(summary = "Document 북마크 토글",
            description = "자료실 게시글 북마크를 추가하거나 제거합니다.\n\n" +
                    "**동작:**\n" +
                    "- 북마크가 없는 경우: 북마크 추가 → true 반환\n" +
                    "- 북마크가 있는 경우: 북마크 제거 → false 반환\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**응답:**\n" +
                    "- true: 북마크 추가됨\n" +
                    "- false: 북마크 제거됨\n\n" +
                    "**활용:**\n" +
                    "- 나중에 다시 보기 위한 북마크\n" +
                    "- 내가 북마크한 자료실 목록 조회 가능")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> toggleBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("Document 북마크 토글 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.toggleBookmark(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }

    @Operation(summary = "Document 북마크 여부 확인",
            description = "사용자가 해당 게시글을 북마크했는지 확인합니다.\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**응답:**\n" +
                    "- true: 북마크 되어 있음\n" +
                    "- false: 북마크 안 되어 있음\n\n" +
                    "**활용:**\n" +
                    "- 게시글 상세 페이지에서 북마크 버튼 상태 표시\n" +
                    "- UI에서 북마크 아이콘 활성화 여부 결정")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{uuid}/bookmark")
    public ApiResponse<Boolean> checkBookmark(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        log.info("Document 북마크 확인 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        boolean isBookmarked = boardBookmarkService.isBookmarked(uuid, userDetails.getUsername());

        return ApiResponse.success(isBookmarked);
    }
}
