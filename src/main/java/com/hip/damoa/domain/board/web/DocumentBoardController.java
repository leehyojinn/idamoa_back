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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "1011. Document Board", description = "자료실 API (BoardType: DOCUMENT) - 파일 업로드/다운로드, 필터 기능, 북마크 지원, 유료 파일 지원")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/document")
public class DocumentBoardController {

    private final DocumentBoardService documentBoardService;
    private final BoardBookmarkService boardBookmarkService;

    @Operation(summary = "Document 게시글 생성",
            description = """
                    새로운 자료실 게시글을 생성합니다.

                    ## 파일 업로드 프로세스
                    1. `/api/files/presigned` 호출 → Presigned URL 획득
                    2. S3로 파일 직접 업로드 (PUT 요청)
                    3. `/api/files/complete` 호출 → 파일 UUID 획득
                    4. **이 API 호출** → 획득한 파일 UUID를 전송

                    ## 유료 파일 설정 방식

                    ### 방식 A: 개별 가격 설정 (권장) ⭐
                    파일마다 다른 가격을 설정할 수 있습니다.

                    ```json
                    {
                      "title": "병원 인테리어 설계도면 모음",
                      "content": "치과, 안과, 피부과 인테리어 설계도면입니다.",
                      "files": [
                        {"uuid": "파일1-uuid", "isPaid": true, "price": 5000},
                        {"uuid": "파일2-uuid", "isPaid": true, "price": 3000},
                        {"uuid": "파일3-uuid", "isPaid": false, "price": 0}
                      ],
                      "thumbnailUuid": "썸네일-uuid"
                    }
                    ```

                    - files 배열을 사용하면 fileUuids, isPaid, price 필드는 무시됩니다
                    - 각 파일별로 유료/무료 및 가격을 개별 설정 가능
                    - isPaid: true이고 price > 0인 파일만 유료로 처리

                    ### 방식 B: 일괄 가격 설정 (레거시)
                    모든 파일에 동일한 가격을 적용합니다.

                    ```json
                    {
                      "title": "병원 인테리어 설계도면",
                      "content": "50평 규모 치과 인테리어 설계도면입니다.",
                      "fileUuids": ["파일1-uuid", "파일2-uuid"],
                      "isPaid": true,
                      "price": 5000,
                      "thumbnailUuid": "썸네일-uuid"
                    }
                    ```

                    ## 필수 정보
                    - title: 제목 (최대 200자)
                    - content: 내용 (최대 5000자)
                    - files 또는 fileUuids: 파일 정보 (최소 1개 이상)

                    ## 선택 정보
                    - categoryId: 카테고리 ID
                    - thumbnailUuid: 썸네일 이미지 UUID
                    - filterOptionIds: 필터 옵션 ID 배열
                    - tags: 태그 배열
                    - isPublished: 즉시 게시 여부 (기본: true)
                    - isPrivate: 비공개 여부 (기본: false)

                    ## 권한
                    - 로그인 필수
                    """)
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
            description = """
                    특정 자료실 게시글을 조회합니다.

                    ## 응답 포함 정보
                    - 제목, 내용, 파일 목록
                    - 작성자 정보 (이름, 프로필 이미지)
                    - 조회수, 좋아요 수, 북마크 수, 다운로드 수
                    - 썸네일 이미지 URL (있는 경우)
                    - **유료 파일 여부 및 가격** (⭐ 중요)
                    - 필터 옵션 정보
                    - 태그 목록
                    - 로그인한 경우: 북마크 여부 포함

                    ## 파일 가격 정보 확인 방법

                    ### 방법 1: files 배열에서 확인 (권장)
                    각 파일의 `isPaid`, `price` 필드로 확인:
                    ```json
                    "files": [
                      {"uuid": "...", "isPaid": true, "price": 5000, ...},
                      {"uuid": "...", "isPaid": false, "price": 0, ...}
                    ]
                    ```

                    ### 방법 2: filePrices 맵에서 확인
                    파일 UUID를 키로 가격 조회:
                    ```json
                    "filePrices": {
                      "550e8400-...": 5000,
                      "660e9500-...": 3000
                    }
                    ```

                    ## 파일 다운로드
                    - 무료 파일: 누구나 다운로드 가능
                    - 유료 파일: 크레딧 결제 후 다운로드 가능
                    - 다운로드 API: `POST /api/files/{fileUuid}/download`

                    ## 권한
                    - 비로그인 사용자도 조회 가능
                    - 비공개 게시글은 작성자만 조회 가능
                    """)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "uuid": "550e8400-e29b-41d4-a716-446655440000",
                                "title": "병원 인테리어 설계도면 모음",
                                "content": "치과, 안과, 피부과 인테리어 설계도면입니다.",
                                "boardType": "DOCUMENT",
                                "categoryId": 1,
                                "categoryName": "설계도면",
                                "files": [
                                  {
                                    "uuid": "file-uuid-001",
                                    "originalFilename": "치과_설계도면.dwg",
                                    "fileUrl": "https://s3.../치과_설계도면.dwg",
                                    "fileSize": 2048576,
                                    "mimeType": "application/octet-stream",
                                    "fileExtension": "dwg",
                                    "isPaid": true,
                                    "price": 5000
                                  },
                                  {
                                    "uuid": "file-uuid-002",
                                    "originalFilename": "안과_설계도면.dwg",
                                    "fileUrl": "https://s3.../안과_설계도면.dwg",
                                    "fileSize": 1536000,
                                    "mimeType": "application/octet-stream",
                                    "fileExtension": "dwg",
                                    "isPaid": true,
                                    "price": 3000
                                  },
                                  {
                                    "uuid": "file-uuid-003",
                                    "originalFilename": "미리보기.pdf",
                                    "fileUrl": "https://s3.../미리보기.pdf",
                                    "fileSize": 512000,
                                    "mimeType": "application/pdf",
                                    "fileExtension": "pdf",
                                    "isPaid": false,
                                    "price": 0
                                  }
                                ],
                                "thumbnail": {
                                  "uuid": "thumb-uuid-001",
                                  "originalFilename": "thumbnail.jpg",
                                  "fileUrl": "https://s3.../thumbnail.jpg",
                                  "fileSize": 102400,
                                  "isPaid": false,
                                  "price": 0
                                },
                                "isPaid": true,
                                "price": 5000,
                                "filePrices": {
                                  "file-uuid-001": 5000,
                                  "file-uuid-002": 3000
                                },
                                "viewCount": 150,
                                "likeCount": 25,
                                "commentCount": 10,
                                "downloadCount": 45,
                                "isPinned": false,
                                "isFeatured": false,
                                "isPublished": true,
                                "publishedAt": "2024-01-15T10:30:00",
                                "filterOptions": [],
                                "tags": ["인테리어", "설계도면", "병원"],
                                "userId": 1,
                                "userEmail": "user@example.com",
                                "userName": "홍길동",
                                "createdAt": "2024-01-15T10:30:00",
                                "updatedAt": "2024-01-15T10:30:00",
                                "isBookmarked": false,
                                "hasDownloaded": false,
                                "isDeleted": false
                              },
                              "errorCode": null,
                              "message": null
                            }
                            """)
            )
    )
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
            description = """
                    자료실 검색 및 목록 조회 통합 API입니다.

                    ## 검색 조건 (모두 선택적, 복합 검색 가능)
                    - keyword: 제목, 내용, 태그에서 검색
                    - filterOptionIds: 필터 옵션 ID 배열 (예: 파일 형식, 카테고리)
                    - onlyBookmarked: true 설정 시 북마크한 게시글만 조회 (로그인 필요)
                    - onlyMyPosts: true 설정 시 내가 작성한 게시글만 조회 (로그인 필요)

                    ## 응답 - 파일 가격 정보
                    각 게시글의 files 배열에서 파일별 가격 정보 확인:
                    ```json
                    "files": [
                      {"uuid": "...", "isPaid": true, "price": 5000, ...},
                      {"uuid": "...", "isPaid": false, "price": 0, ...}
                    ]
                    ```

                    ## 페이지네이션
                    - size: 페이지당 항목 수 (기본 20)
                    - page: 페이지 번호 (0부터 시작)
                    - sort: 정렬 기준 (기본: publishedAt,DESC - 최신순)

                    ## 정렬 옵션
                    - publishedAt,DESC: 최신순 (기본값)
                    - viewCount,DESC: 조회수 높은 순
                    - createdAt,DESC: 생성일 최신순
                    """)
    @GetMapping("/search")
    public ApiResponse<Page<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) List<Long> filterOptionIds,
            @RequestParam(required = false) Boolean onlyBookmarked,
            @RequestParam(required = false) Boolean onlyMyPosts,
            @AuthenticationPrincipal(errorOnInvalidType = false) UserDetails userDetails,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("Document 게시글 검색 요청: keyword={}, tags={}, filterOptionIds={}, onlyBookmarked={}, onlyMyPosts={}, userEmail={}",
                 keyword, tags != null ? String.join(",", tags) : null, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail);

        Page<DocumentResponse> response = documentBoardService.searchDocuments(
                keyword, tags, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail, pageable);

        return ApiResponse.success(response);
    }

    @Operation(summary = "내가 작성한 Document 목록 (마이페이지)",
            description = "로그인한 사용자가 작성한 자료실 게시글 목록을 조회합니다.\n\n" +
                    "**조회 대상:**\n" +
                    "- 본인이 작성한 Document 게시글만 조회\n" +
                    "- 삭제되지 않은 게시글만 포함\n" +
                    "- 공개/비공개 상태 모두 포함\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신 작성순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: viewCount, downloadCount\n\n" +
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
            description = """
                    자료실 게시글을 수정합니다.

                    ## 수정 가능 항목
                    - 제목, 내용
                    - 파일 목록 (추가/삭제 가능)
                    - 썸네일 이미지
                    - 카테고리
                    - 유료 파일 설정 (가격 변경 가능)
                    - 필터 옵션, 태그

                    ## 유료 파일 설정 방식

                    ### 방식 A: 개별 가격 설정 (권장) ⭐
                    파일마다 다른 가격을 설정할 수 있습니다.

                    ```json
                    {
                      "title": "수정된 제목",
                      "content": "수정된 내용",
                      "files": [
                        {"uuid": "파일1-uuid", "isPaid": true, "price": 8000},
                        {"uuid": "파일2-uuid", "isPaid": true, "price": 5000},
                        {"uuid": "새파일-uuid", "isPaid": false, "price": 0}
                      ]
                    }
                    ```

                    ### 방식 B: 일괄 가격 설정 (레거시)
                    모든 파일에 동일한 가격을 적용합니다.

                    ```json
                    {
                      "title": "수정된 제목",
                      "fileUuids": ["파일1-uuid", "파일2-uuid"],
                      "isPaid": true,
                      "price": 5000
                    }
                    ```

                    ## 권한
                    - 작성자 본인만 수정 가능
                    - 다른 사용자가 수정 시도 시 403 Forbidden

                    ## 주의사항
                    - 수정 시 updatedAt 자동 갱신
                    - 파일 변경 시 새로운 파일 업로드 후 UUID 전송
                    - 유료 파일 가격 변경 시 기존 구매자에게는 영향 없음
                    """)
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
