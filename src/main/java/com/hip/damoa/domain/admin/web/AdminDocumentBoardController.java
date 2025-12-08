package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.board.service.BoardService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 관리자 자료실 관리 API
 */
@Tag(name = "1905. Admin - Document Board", description = "관리자 자료실 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards/document")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentBoardController {

    private final DocumentBoardService documentBoardService;
    private final BoardService boardService;
    private final BoardRepository boardRepository;

    /**
     * 자료실 게시글 목록 조회 (관리자용 - 미게시 포함)
     */
    @Operation(summary = "[관리자] 자료실 게시글 목록 조회",
            description = "자료실 게시글 목록을 조회합니다 (미게시 포함).\n\n" +
                    "**검색 필터**\n" +
                    "- keyword: 제목/내용으로 검색 (선택)\n" +
                    "- categoryId: 카테고리별 필터 (선택)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: publishedAt DESC (최신순)\n" +
                    "- 사용법: sort=publishedAt,desc 또는 sort=publishedAt,asc\n" +
                    "- 기타 옵션: createdAt, viewCount, likeCount")
    @GetMapping
    public ApiResponse<Page<DocumentResponse>> getAllDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 자료실 게시글 목록 조회: adminEmail={}, keyword={}",
                userDetails.getUsername(), keyword);

        // 관리자는 미게시 포함 전체 조회
        Page<DocumentResponse> response;
        if (keyword != null && !keyword.isEmpty()) {
            response = documentBoardService.searchDocument(keyword, pageable);
        } else {
            response = documentBoardService.getDocumentList(pageable);
        }
        return ApiResponse.success(response);
    }

    /**
     * 자료실 게시글 상세 조회
     */
    @Operation(summary = "[관리자] 자료실 게시글 상세 조회",
            description = "자료실 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/{documentUuid}")
    public ApiResponse<DocumentResponse> getDocumentDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid) {
        log.info("[관리자] 자료실 게시글 상세 조회: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);
        DocumentResponse response = documentBoardService.getDocument(documentUuid, userDetails.getUsername());
        return ApiResponse.success(response);
    }

    /**
     * 자료실 게시글 생성
     */
    @Operation(summary = "[관리자] 자료실 게시글 생성",
            description = """
                    새로운 자료실 게시글을 생성합니다.

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

                    ### 방식 B: 일괄 가격 설정 (레거시)
                    모든 파일에 동일한 가격을 적용합니다.

                    ```json
                    {
                      "title": "병원 인테리어 설계도면",
                      "content": "50평 규모 치과 인테리어 설계도면입니다.",
                      "fileUuids": ["파일1-uuid", "파일2-uuid"],
                      "isPaid": true,
                      "price": 5000
                    }
                    ```
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> createDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentCreateRequest request) {
        log.info("[관리자] 자료실 게시글 생성: adminEmail={}, title={}",
                userDetails.getUsername(), request.getTitle());
        DocumentResponse response = documentBoardService.createDocument(userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * 자료실 게시글 수정
     */
    @Operation(summary = "[관리자] 자료실 게시글 수정",
            description = """
                    자료실 게시글을 수정합니다.

                    ## 유료 파일 설정 방식

                    ### 방식 A: 개별 가격 설정 (권장) ⭐
                    ```json
                    {
                      "title": "수정된 제목",
                      "files": [
                        {"uuid": "파일1-uuid", "isPaid": true, "price": 8000},
                        {"uuid": "파일2-uuid", "isPaid": false, "price": 0}
                      ]
                    }
                    ```

                    ### 방식 B: 일괄 가격 설정 (레거시)
                    ```json
                    {
                      "title": "수정된 제목",
                      "fileUuids": ["파일1-uuid", "파일2-uuid"],
                      "isPaid": true,
                      "price": 5000
                    }
                    ```

                    **주의**: 가격 변경 시 기존 구매자에게는 영향 없음
                    """)
    @PutMapping("/{documentUuid}")
    public ApiResponse<DocumentResponse> updateDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid,
            @Valid @RequestBody DocumentUpdateRequest request) {
        log.info("[관리자] 자료실 게시글 수정: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);
        DocumentResponse response = documentBoardService.updateDocument(documentUuid, userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * 자료실 게시글 삭제
     */
    @Operation(summary = "[관리자] 자료실 게시글 삭제",
            description = "자료실 게시글을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{documentUuid}")
    public ApiResponse<Void> deleteDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid) {
        log.info("[관리자] 자료실 게시글 삭제: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);
        Board board = boardService.getBoard(documentUuid);
        board.softDelete();
        return ApiResponse.success();
    }

    /**
     * 자료실 게시글 게시/게시 취소 토글
     */
    @Operation(summary = "[관리자] 자료실 게시글 게시/취소",
            description = "자료실 게시글의 게시 상태를 변경합니다.")
    @PatchMapping("/{documentUuid}/publish")
    public ApiResponse<Void> togglePublish(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid) {
        log.info("[관리자] 자료실 게시 상태 변경: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);

        Board board = boardService.getBoard(documentUuid);
        if (board.getIsPublished()) {
            board.unpublish();
        } else {
            board.publish();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }

    /**
     * 자료실 게시글 고정/고정 해제 토글
     */
    @Operation(summary = "[관리자] 자료실 게시글 고정/해제",
            description = "자료실 게시글의 고정 상태를 변경합니다.")
    @PatchMapping("/{documentUuid}/pin")
    public ApiResponse<Void> togglePin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid) {
        log.info("[관리자] 자료실 고정 상태 변경: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);

        Board board = boardService.getBoard(documentUuid);
        if (board.getIsPinned()) {
            board.unpin();
        } else {
            board.pin();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }

    /**
     * 자료실 게시글 추천/추천 해제 토글
     */
    @Operation(summary = "[관리자] 자료실 게시글 추천/해제",
            description = "자료실 게시글의 추천 상태를 변경합니다.")
    @PatchMapping("/{documentUuid}/feature")
    public ApiResponse<Void> toggleFeature(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID documentUuid) {
        log.info("[관리자] 자료실 추천 상태 변경: adminEmail={}, documentUuid={}",
                userDetails.getUsername(), documentUuid);

        Board board = boardService.getBoard(documentUuid);
        if (board.getIsFeatured()) {
            board.unfeature();
        } else {
            board.feature();
        }
        boardRepository.save(board);
        return ApiResponse.success();
    }
}
